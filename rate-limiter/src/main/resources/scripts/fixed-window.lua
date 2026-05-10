local key = KEYS[1]
local max_permits = tonumber(ARGV[1])
local window_millis = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local permits = tonumber(ARGV[4])

local count = redis.call('GET', key)
if not count then
    redis.call('SET', key, permits, 'PX', window_millis)
    return 1
end

count = tonumber(count)
if count + permits <= max_permits then
    redis.call('INCRBY', key, permits)
    redis.call('PEXPIRE', key, window_millis)
    return 1
end
return 0
