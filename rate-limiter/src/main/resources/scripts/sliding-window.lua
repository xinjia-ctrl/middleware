local key = KEYS[1]
local max_permits = tonumber(ARGV[1])
local window_millis = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local permits = tonumber(ARGV[4])

local window_start = now - window_millis

redis.call('ZREMRANGEBYSCORE', key, 0, window_start)

local count = redis.call('ZCARD', key)

if count + permits <= max_permits then
    local member = now .. ':' .. math.random()
    redis.call('ZADD', key, now, member)
    redis.call('PEXPIRE', key, window_millis * 2)
    return 1
end
return 0
