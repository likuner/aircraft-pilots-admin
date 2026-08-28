#!/bin/bash
# ============================================================
# 无人机驾驶员管理后台 - 全业务闭环冒烟测试 v3
# 契约：审核动作统一 PASS/REJECT（核查 PASS/FAIL）
# ============================================================
set -u
BASE=http://localhost:8080
PASS=0; FAIL=0
GREEN='\033[32m'; RED='\033[31m'; NC='\033[0m'
PY=/Users/likun/.workbuddy/binaries/python/versions/3.13.12/bin/python3

ok()   { PASS=$((PASS+1)); printf "${GREEN}[PASS]${NC} %s\n" "$1"; }
bad()  { FAIL=$((FAIL+1)); printf "${RED}[FAIL]${NC} %s\n  -> %s\n" "$1" "${2:-}"; }

# 精确判断 JSON 响应 code==200
is_ok() { echo "$1" | $PY -c "import sys,json;d=json.load(sys.stdin);print(1 if d.get('code')==200 else 0)" 2>/dev/null; }

# ---------- 1. 登录 ----------
echo "== 1. 登录 =="
CAP=$(curl -s $BASE/api/auth/captcha)
KEY=$(echo "$CAP" | grep -o '"captchaKey":"[^"]*' | cut -d'"' -f4)
CODE=$(docker exec -i uav-redis redis-cli -a redis123 --no-auth-warning GET "uav:captcha:$KEY" 2>/dev/null)
TOKEN=$(curl -s -X POST $BASE/api/auth/login -H 'Content-Type: application/json' \
  -d "{\"username\":\"admin\",\"password\":\"123456\",\"captchaKey\":\"$KEY\",\"captchaCode\":\"$CODE\"}" \
  | $PY -c "import sys,json;print(json.load(sys.stdin)['data']['accessToken'])")
if [ -z "$TOKEN" ]; then bad "admin 登录" "未获取到 token"; exit 1; else ok "admin 登录 (${TOKEN:0:20}...)"; fi
AUTH="Authorization: Bearer $TOKEN"
JSON="Content-Type: application/json"

# ---------- 0. 清理上一轮 admin 的档案数据（uk_user_id/uk_id_card 唯一约束会冲突） ----------
docker exec -i uav-mysql mysql -uroot -proot123 uav_admin -e "
DELETE FROM exm_score WHERE registration_id IN (SELECT id FROM exm_registration WHERE student_profile_id IN (SELECT id FROM stu_pilot_profile WHERE user_id=1));
DELETE FROM cer_certificate_apply WHERE registration_id IN (SELECT id FROM exm_registration WHERE student_profile_id IN (SELECT id FROM stu_pilot_profile WHERE user_id=1));
DELETE FROM exm_registration WHERE student_profile_id IN (SELECT id FROM stu_pilot_profile WHERE user_id=1);
DELETE FROM stu_pilot_profile WHERE user_id=1;" 2>/dev/null && echo "[cleanup] 已清理 admin 上一轮档案数据"

gget() {
  if [ -n "${2:-}" ]; then curl -s -G "$BASE$1" -H "$AUTH" --data-urlencode "$2"; else curl -s "$BASE$1" -H "$AUTH"; fi
}
find_id() { # $1=python-expr；JSON 由 stdin 传入（管道 stdin 会被 heredoc 覆盖，故用 -c + argv 传表达式，避免引号嵌套）
  local expr="$1"
  $PY -c '
import sys, json
d = json.load(sys.stdin)
rows = d.get("data", {}).get("rows", []) if isinstance(d.get("data"), dict) else []
for r in rows:
    if eval(sys.argv[1]):
        print(r.get("id", ""))
        break
' "$expr" 2>/dev/null
}

TS=$(date +%s)$((RANDOM % 100))

# ---------- 2. 考生档案 ----------
echo "== 2. 考生档案 =="
R=$(curl -s -X POST $BASE/api/student/profiles -H "$AUTH" -H "$JSON" -d "{\"name\":\"张${TS: -4}\",\"idCard\":\"11010119900101${TS: -4}\",\"gender\":1,\"birthDate\":\"1990-01-01\",\"phone\":\"1380013${TS: -4}\",\"pilotType\":\"多旋翼\",\"aircraftModel\":\"DJI M350\",\"flyingHours\":125.5,\"examCategory\":\"驾驶员\",\"education\":\"本科\",\"emergencyContact\":\"李四\"}")
[ "$(is_ok "$R")" = "1" ] && ok "新增考生档案" || bad "新增考生档案" "$R"
LIST=$(gget /api/student/profiles "keyword=张${TS: -4}")
PROFILE_ID=$(echo "$LIST" | find_id "'张${TS: -4}' in r.get('name','')")
[ -n "$PROFILE_ID" ] && ok "档案回捞 id=$PROFILE_ID" || bad "档案回捞" "$LIST"

# ---------- 3. 考试计划 ----------
echo "== 3. 考试计划 =="
PLAN_NAME="冒烟计划${TS: -4}"
R=$(curl -s -X POST $BASE/api/exam/plans -H "$AUTH" -H "$JSON" -d "{\"planName\":\"$PLAN_NAME\",\"examType\":\"驾驶员\",\"startDate\":\"2026-09-01\",\"endDate\":\"2026-09-30\",\"region\":\"广东省\",\"description\":\"冒烟\"}")
[ "$(is_ok "$R")" = "1" ] && ok "新增考试计划" || bad "新增考试计划" "$R"
PLAN_ID=$(gget /api/exam/plans "keyword=$PLAN_NAME" | find_id "r.get('planName','')=='$PLAN_NAME'")
[ -n "$PLAN_ID" ] && ok "计划回捞 id=$PLAN_ID" || bad "计划回捞"
R=$(curl -s -X PUT "$BASE/api/exam/plans/$PLAN_ID/publish" -H "$AUTH")
[ "$(is_ok "$R")" = "1" ] && ok "发布考试计划" || bad "发布考试计划" "$R"

# ---------- 4. 考场 ----------
echo "== 4. 考场 =="
ROOM_CODE="RM${TS: -5}"
R=$(curl -s -X POST $BASE/api/exam/rooms -H "$AUTH" -H "$JSON" -d "{\"roomCode\":\"$ROOM_CODE\",\"roomName\":\"冒烟考场\",\"location\":\"广州白云\",\"capacity\":60,\"status\":1}")
[ "$(is_ok "$R")" = "1" ] && ok "新增考场" || bad "新增考场" "$R"
ROOM_ID=$(gget /api/exam/rooms "keyword=$ROOM_CODE" | find_id "r.get('roomCode','')=='$ROOM_CODE'")
[ -n "$ROOM_ID" ] && ok "考场回捞 id=$ROOM_ID" || bad "考场回捞"

# ---------- 5. 考试场次 ----------
echo "== 5. 考试场次 =="
SES_NAME="冒烟场次${TS: -4}"
R=$(curl -s -X POST $BASE/api/exam/sessions -H "$AUTH" -H "$JSON" -d "{\"planId\":$PLAN_ID,\"sessionName\":\"$SES_NAME\",\"examType\":\"驾驶员\",\"examDate\":\"2026-10-10\",\"startTime\":\"09:00\",\"endTime\":\"11:00\",\"location\":\"广州白云\",\"roomId\":$ROOM_ID,\"fullScore\":100,\"passScore\":80,\"capacity\":50}")
[ "$(is_ok "$R")" = "1" ] && ok "新增场次" || bad "新增场次" "$R"
SESSION_ID=$(gget /api/exam/sessions "sessionName=$SES_NAME" | find_id "r.get('sessionName','')=='$SES_NAME'")
[ -n "$SESSION_ID" ] && ok "场次回捞 id=$SESSION_ID" || bad "场次回捞"
R=$(curl -s -X PUT "$BASE/api/exam/sessions/$SESSION_ID/publish" -H "$AUTH")
[ "$(is_ok "$R")" = "1" ] && ok "发布场次" || bad "发布场次" "$R"

# ---------- 6. 批次 ----------
echo "== 6. 考试批次 =="
R=$(curl -s -X POST $BASE/api/exam/batches -H "$AUTH" -H "$JSON" -d "{\"sessionId\":$SESSION_ID,\"roomId\":$ROOM_ID,\"batchTime\":\"2026-10-10T09:00:00\",\"capacity\":50}")
[ "$(is_ok "$R")" = "1" ] && ok "新增批次" || bad "新增批次" "$R"
BATCH_ID=$(gget "/api/exam/batches" "sessionId=$SESSION_ID" | find_id "r.get('sessionId')==$SESSION_ID")
[ -n "$BATCH_ID" ] && ok "批次回捞 id=$BATCH_ID" || bad "批次回捞"

# ---------- 7. 报名（Lua 名额扣减） ----------
echo "== 7. 报名 =="
R=$(curl -s -X POST $BASE/api/exam/registrations -H "$AUTH" -H "$JSON" -d "{\"sessionId\":$SESSION_ID,\"studentProfileId\":$PROFILE_ID}")
[ "$(is_ok "$R")" = "1" ] && ok "提交报名(名额扣减)" || bad "提交报名" "$R"
REG_ID=$(gget /api/exam/registrations "sessionId=$SESSION_ID" | find_id "r.get('sessionId')==$SESSION_ID")
[ -n "$REG_ID" ] && ok "报名回捞 id=$REG_ID" || bad "报名回捞"
R=$(curl -s -X PUT "$BASE/api/exam/registrations/$REG_ID/approve" -H "$AUTH")
[ "$(is_ok "$R")" = "1" ] && ok "报名审核通过" || bad "报名审核通过" "$R"
R=$(curl -s -X PUT "$BASE/api/exam/registrations/$REG_ID/arrange?batchId=$BATCH_ID" -H "$AUTH")
[ "$(is_ok "$R")" = "1" ] && ok "报名排考" || bad "报名排考" "$R"

# ---------- 8. 成绩 ----------
echo "== 8. 成绩录入与判定 =="
R=$(curl -s -X POST $BASE/api/exam/scores -H "$AUTH" -H "$JSON" -d "{\"registrationId\":$REG_ID,\"examType\":\"驾驶员\",\"score\":92.5}")
[ "$(is_ok "$R")" = "1" ] && ok "录入成绩" || bad "录入成绩" "$R"
SCORE_ID=$(gget "/api/exam/scores" "sessionId=$SESSION_ID" | find_id "r.get('registrationId')==$REG_ID")
[ -n "$SCORE_ID" ] && ok "成绩回捞 id=$SCORE_ID" || bad "成绩回捞"
R=$(curl -s -X PUT "$BASE/api/exam/scores/$SCORE_ID/submit" -H "$AUTH")
[ "$(is_ok "$R")" = "1" ] && ok "提交成绩" || bad "提交成绩" "$R"
R=$(curl -s -X PUT "$BASE/api/exam/scores/$SCORE_ID/audit" -H "$AUTH" -H "$JSON" -d '{"action":"PASS","comment":"成绩真实有效"}')
[ "$(is_ok "$R")" = "1" ] && ok "审核成绩(PASS)" || bad "审核成绩" "$R"
PASS_STATUS=$(gget "/api/exam/scores" "sessionId=$SESSION_ID" | $PY -c "
import sys,json
d=json.load(sys.stdin)
for r in d['data']['rows']:
    if r.get('registrationId')==$REG_ID: print(r.get('passStatus',''));break" 2>/dev/null)
[ "$PASS_STATUS" = "PASS" ] && ok "自动判定 PASS (92.5>=80)" || bad "自动判定" "passStatus=$PASS_STATUS"

# ---------- 9. 合格证 + MQ 签发 ----------
echo "== 9. 合格证（MQ 异步签发） =="
R=$(curl -s -X POST $BASE/api/cert/applications -H "$AUTH" -H "$JSON" -d "{\"registrationId\":$REG_ID,\"scoreId\":$SCORE_ID,\"certificateType\":\"驾驶员合格证\"}")
[ "$(is_ok "$R")" = "1" ] && ok "提交证书申请" || bad "提交证书申请" "$R"
APPLY_ID=$(gget /api/cert/applications "status=PENDING_AUDIT" | find_id "r.get('registrationId')==$REG_ID")
[ -n "$APPLY_ID" ] && ok "申请回捞 id=$APPLY_ID" || bad "申请回捞"
R=$(curl -s -X PUT "$BASE/api/cert/applications/$APPLY_ID/audit" -H "$AUTH" -H "$JSON" -d '{"action":"PASS","comment":"符合颁发条件"}')
[ "$(is_ok "$R")" = "1" ] && ok "证书申请审核(PASS)" || bad "证书申请审核" "$R"
sleep 3
CERT_NO=$(gget /api/cert/certificates "status=VALID" | $PY -c "
import sys,json
d=json.load(sys.stdin)
for r in d['data']['rows']:
    if r.get('applyId')==$APPLY_ID: print(r.get('certNo',''));break" 2>/dev/null)
[ -n "$CERT_NO" ] && ok "MQ 异步签发证书: $CERT_NO" || bad "证书 MQ 签发"

# ---------- 10. 机构认证 ----------
echo "== 10. 机构认证 =="
INST_NAME="冒烟机构${TS: -4}"
R=$(curl -s -X POST $BASE/api/institution/institutions -H "$AUTH" -H "$JSON" -d "{\"instName\":\"$INST_NAME\",\"creditCode\":\"91${TS: -6}\",\"orgType\":\"企业\",\"legalPerson\":\"王五\",\"registeredCapital\":500,\"address\":\"广州天河\",\"contactName\":\"王五\",\"contactPhone\":\"13700137000\",\"email\":\"smoke@test.com\",\"businessScope\":\"无人机培训\"}")
[ "$(is_ok "$R")" = "1" ] && ok "新增机构" || bad "新增机构" "$R"
INST_ID=$(gget /api/institution/institutions "keyword=$INST_NAME" | find_id "'$INST_NAME' in r.get('instName','')")
[ -n "$INST_ID" ] && ok "机构回捞 id=$INST_ID" || bad "机构回捞"

R=$(curl -s -X POST $BASE/api/institution/applications -H "$AUTH" -H "$JSON" -d "{\"institutionId\":$INST_ID,\"applyType\":\"初始认证\",\"category\":\"训练机构资质\"}")
[ "$(is_ok "$R")" = "1" ] && ok "提交认证申请" || bad "提交认证申请" "$R"
APP_ID=$(gget "/api/institution/applications" "institutionId=$INST_ID" | find_id "r.get('institutionId')==$INST_ID")
[ -n "$APP_ID" ] && ok "认证申请回捞 id=$APP_ID" || bad "认证申请回捞"
R=$(curl -s -X PUT "$BASE/api/institution/applications/$APP_ID/materials" -H "$AUTH" -H "$JSON" -d '[{"materialType":"营业执照","fileName":"license.pdf","fileUrl":"/files/license.pdf"}]')
[ "$(is_ok "$R")" = "1" ] && ok "提交认证材料" || bad "提交认证材料" "$R"
R=$(curl -s -X PUT "$BASE/api/institution/applications/$APP_ID/review-material" -H "$AUTH" -H "$JSON" -d '{"result":"PASS","comment":"材料齐全","reviewStep":1}')
[ "$(is_ok "$R")" = "1" ] && ok "材料审查(PASS)" || bad "材料审查" "$R"
R=$(curl -s -X PUT "$BASE/api/institution/applications/$APP_ID/assign-inspection" -H "$AUTH" -H "$JSON" -d "{\"inspectorId\":2,\"inspectionDate\":\"2026-10-15\",\"address\":\"广州天河\"}")
[ "$(is_ok "$R")" = "1" ] && ok "指派现场核查" || bad "指派现场核查" "$R"
INSP_ID=$(gget "/api/institution/inspections" "applyId=$APP_ID" | find_id "r.get('applyId')==$APP_ID")
[ -n "$INSP_ID" ] && ok "核查任务回捞 id=$INSP_ID" || bad "核查任务回捞"
R=$(curl -s -X PUT "$BASE/api/institution/inspections/$INSP_ID/complete" -H "$AUTH" -H "$JSON" -d '{"result":"PASS","summary":"场地与设备符合要求"}')
[ "$(is_ok "$R")" = "1" ] && ok "完成现场核查(PASS)" || bad "完成现场核查" "$R"
R=$(curl -s -X PUT "$BASE/api/institution/applications/$APP_ID/qualify" -H "$AUTH" -H "$JSON" -d '{"evaluationScore":92,"suggestion":"同意","result":"PASS"}')
[ "$(is_ok "$R")" = "1" ] && ok "资质评定(发证)" || bad "资质评定" "$R"
sleep 1
QUAL_NO=$(gget "/api/institution/qualifications" "institutionId=$INST_ID" | $PY -c "
import sys,json
d=json.load(sys.stdin)
for r in d['data']['rows']:
    if r.get('institutionId')==$INST_ID: print(r.get('qualificationNo',''));break" 2>/dev/null)
[ -n "$QUAL_NO" ] && ok "资质证书颁发: $QUAL_NO" || bad "资质发证"

# ---------- 11. Dashboard ----------
echo "== 11. Dashboard 统计 =="
R=$(curl -s $BASE/api/dashboard/stats -H "$AUTH")
[ "$(is_ok "$R")" = "1" ] && ok "首页统计" || bad "首页统计" "$R"

# ---------- 汇总 ----------
echo ""
echo "=========================================="
echo "  PASS: $PASS    FAIL: $FAIL"
echo "=========================================="
[ $FAIL -eq 0 ] && exit 0 || exit 1
