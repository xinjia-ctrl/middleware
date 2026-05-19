local key = KEYS[1]
local rate = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local permits = tonumber(ARGV[4])

local tokens = redis.call('GET', key .. ':tokens')
local ts = redis.call('GET', key .. ':ts')

local ttl = math.ceil(capacity / rate * 1000 * 2)

if not tokens then
    local remaining = capacity - permits
    if remaining >= 0 then
        redis.call('SET', key .. ':tokens', remaining, 'PX', ttl)
        redis.call('SET', key .. ':ts', now, 'PX', ttl)
        return 1
    end
    return 0
end

tokens = tonumber(tokens)
ts = tonumber(ts)

local elapsed = math.max(0, now - ts)
local new_tokens = math.min(capacity, tokens + (elapsed * rate / 1000))
local remaining = new_tokens - permits

if remaining >= 0 then
    redis.call('SET', key .. ':tokens', remaining, 'PX', ttl)
    redis.call('SET', key .. ':ts', now, 'PX', ttl)
    return 1
end

redis.call('SET', key .. ':tokens', new_tokens, 'PX', ttl)
redis.call('SET', key .. ':ts', now, 'PX', ttl)
return 0
