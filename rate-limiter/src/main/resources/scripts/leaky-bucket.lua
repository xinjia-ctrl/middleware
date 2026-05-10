local key = KEYS[1]
local leak_rate = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local permits = tonumber(ARGV[4])

local ttl = math.ceil(capacity / leak_rate * 1000 * 2)

local water = redis.call('GET', key .. ':water')
local ts = redis.call('GET', key .. ':ts')

if not water then
    redis.call('SET', key .. ':water', permits, 'PX', ttl)
    redis.call('SET', key .. ':ts', now, 'PX', ttl)
    return 1
end

water = tonumber(water)
ts = tonumber(ts)

local elapsed = math.max(0, now - ts)
local leaked = elapsed * leak_rate / 1000
water = math.max(0, water - leaked)

if water + permits <= capacity then
    water = water + permits
    redis.call('SET', key .. ':water', water, 'PX', ttl)
    redis.call('SET', key .. ':ts', now, 'PX', ttl)
    return 1
end

redis.call('SET', key .. ':ts', now, 'PX', ttl)
return 0
