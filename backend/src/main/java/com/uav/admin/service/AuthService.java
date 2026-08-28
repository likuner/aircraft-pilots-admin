package com.uav.admin.service;

import com.uav.admin.common.BaseException;
import com.uav.admin.common.Constants;
import com.uav.admin.common.ErrorCode;
import com.uav.admin.dto.CaptchaResponse;
import com.uav.admin.dto.LoginRequest;
import com.uav.admin.dto.LoginResponse;
import com.uav.admin.entity.SysMenu;
import com.uav.admin.entity.SysUser;
import com.uav.admin.mapper.SysMenuMapper;
import com.uav.admin.mapper.SysUserMapper;
import com.uav.admin.security.JwtUtil;
import com.uav.admin.security.LoginUser;
import com.uav.admin.security.SecurityUtils;
import com.uav.admin.security.UserDetailsServiceImpl;
import com.uav.admin.util.CaptchaUtil;
import com.uav.admin.util.RedisUtil;
import com.uav.admin.vo.MenuTreeVO;
import com.uav.admin.vo.UserInfoVO;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 认证服务：验证码 / 登录 / 登出 / 刷新 / 当前用户
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final SysMenuMapper menuMapper;
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public CaptchaResponse captcha() {
        String[] result = CaptchaUtil.generate();
        String key = UUID.randomUUID().toString().replace("-", "");
        redisUtil.set(Constants.CAPTCHA_PREFIX + key, result[0], Duration.ofMinutes(5));
        return new CaptchaResponse(key, result[1]);
    }

    public LoginResponse login(LoginRequest request) {
        // 1. 校验验证码
        if (!StringUtils.hasText(request.getCaptchaKey()) || !StringUtils.hasText(request.getCaptchaCode())) {
            throw new BaseException(ErrorCode.CAPTCHA_ERROR);
        }
        String cached = redisUtil.get(Constants.CAPTCHA_PREFIX + request.getCaptchaKey());
        if (cached == null || !cached.equalsIgnoreCase(request.getCaptchaCode())) {
            throw new BaseException(ErrorCode.CAPTCHA_ERROR);
        }
        redisUtil.delete(Constants.CAPTCHA_PREFIX + request.getCaptchaKey());

        // 2. 认证
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BaseException(ErrorCode.LOGIN_FAIL);
        }
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();

        // 3. 签发 token
        String accessToken = jwtUtil.generateToken(loginUser.getUserId(), loginUser.getUsername(), loginUser.getRoles());
        String refreshToken = jwtUtil.generateRefreshToken(loginUser.getUserId(), loginUser.getUsername());
        redisUtil.set(Constants.REFRESH_PREFIX + loginUser.getUserId(), refreshToken, Duration.ofDays(7));

        // 4. 更新登录信息
        userMapper.updateLastLogin(loginUser.getUserId(), "unknown");
        return new LoginResponse(accessToken, refreshToken);
    }

    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            try {
                Claims claims = jwtUtil.parseToken(token);
                // 拉黑 access token 至其过期
                long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();
                if (ttl > 0) {
                    redisUtil.set(Constants.TOKEN_BLACKLIST_PREFIX + jwtUtil.getJti(claims),
                            "1", Duration.ofMillis(ttl));
                }
            } catch (Exception ignored) {
                // token 已失效无需处理
            }
        }
    }

    public LoginResponse refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BaseException(ErrorCode.TOKEN_INVALID);
        }
        Claims claims = jwtUtil.parseToken(refreshToken);
        Long userId = jwtUtil.getUserId(claims);
        String cached = redisUtil.get(Constants.REFRESH_PREFIX + userId);
        if (cached == null || !cached.equals(refreshToken)) {
            throw new BaseException(ErrorCode.TOKEN_INVALID);
        }
        LoginUser loginUser = userDetailsService.loadUserByUserId(userId);
        String accessToken = jwtUtil.generateToken(userId, loginUser.getUsername(), loginUser.getRoles());
        return new LoginResponse(accessToken, refreshToken);
    }

    public void changePassword(String oldPassword, String newPassword) {
        Long userId = SecurityUtils.getUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BaseException(ErrorCode.PASSWORD_ERROR);
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(update);
    }

    /**
     * 当前用户信息 + 角色 + 权限 + 菜单树
     */
    public UserInfoVO me() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(loginUser.getUserId());
        vo.setUsername(loginUser.getUsername());
        vo.setRealName(loginUser.getRealName());
        vo.setRoles(loginUser.getRoles());
        vo.setPermissions(new ArrayList<>(loginUser.getPermissions()));

        SysUser user = userMapper.selectById(loginUser.getUserId());
        if (user != null) {
            vo.setAvatar(user.getAvatar());
            vo.setPhone(user.getPhone());
            vo.setEmail(user.getEmail());
        }
        // 菜单树（目录+菜单）
        List<SysMenu> menus = menuMapper.selectMenusByUserId(loginUser.getUserId());
        vo.setMenus(buildTree(menus));
        return vo;
    }

    private List<MenuTreeVO> buildTree(List<SysMenu> menus) {
        List<MenuTreeVO> tree = new ArrayList<>();
        for (SysMenu menu : menus) {
            MenuTreeVO vo = toVO(menu);
            if (menu.getParentId() == null || menu.getParentId() == 0) {
                vo.setChildren(collectChildren(menus, menu.getId()));
                tree.add(vo);
            }
        }
        return tree;
    }

    private List<MenuTreeVO> collectChildren(List<SysMenu> menus, Long parentId) {
        List<MenuTreeVO> children = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (parentId.equals(menu.getParentId())) {
                MenuTreeVO vo = toVO(menu);
                vo.setChildren(collectChildren(menus, menu.getId()));
                children.add(vo);
            }
        }
        return children;
    }

    private MenuTreeVO toVO(SysMenu menu) {
        MenuTreeVO vo = new MenuTreeVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setMenuName(menu.getMenuName());
        vo.setMenuType(menu.getMenuType());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setPerms(menu.getPerms());
        vo.setIcon(menu.getIcon());
        vo.setOrderNum(menu.getOrderNum());
        return vo;
    }
}
