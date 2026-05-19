local key = KEYS[1]
local max_permits = tonumber(ARGV[1])
local window_millis = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local permits = tonumber(ARGV[4])

local window_start = now - (now % window_millis)
local current_start = redis.call('HGET', key, 'window_start')
local count = redis.call('HGET', key, 'count')

if not current_start or tonumber(current_start) ~= window_start then
    redis.call('HSET', key, 'window_start', window_start, 'count', permits)
    redis.call('PEXPIRE', key, window_millis * 2)
    return 1
end

count = tonumber(count)
if count + permits <= max_permits then
    redis.call('HINCRBY', key, 'count', permits)
    redis.call('PEXPIRE', key, window_millis * 2)
    return 1
end
return 0
