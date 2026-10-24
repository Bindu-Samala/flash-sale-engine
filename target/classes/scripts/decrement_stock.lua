local current_stock = tonumber(redis.call('get', KEYS[1]))
if not current_stock then
    return -1
end

local quantity = tonumber(ARGV[1])
if current_stock >= quantity then
    redis.call('decrby', KEYS[1], quantity)
    return 1
else
    return 0
end