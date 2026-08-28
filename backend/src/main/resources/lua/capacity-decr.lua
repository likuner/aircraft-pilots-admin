-- 场次/批次名额原子扣减
-- KEYS[1] = capacity key
-- 返回剩余名额；小于 0 表示名额已满（已回补）
local remain = redis.call('DECR', KEYS[1])
if remain < 0 then
  redis.call('INCR', KEYS[1])
  return -1
end
return remain
