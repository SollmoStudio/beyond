var console = require('console')
var db = require('db')
var util = require('util')

var schema = new db.Schema(1, { key: { type: 'string'}, value: {type: 'double'}})
var collection = new Collection("example.keyValue", schema);

exports.insert = function (key, value) {
    return collection.insert({key: key, value: value}).onComplete(console.log);
}

exports.find = function () {
    var queries = Array.prototype.slice.call(arguments, 0).map(function (arg) {
        return db.query().eq("key", arg);
    });
    var emptyQuery = db.query();
    var baseQuery = queries.shift()
    var q = baseQuery.or.apply(baseQuery, queries);
    return collection.find(q).onComplete(function (result, isSuccess) {
        if (isSuccess) {
            console.log(util.format("Find %d keyValue", result.length));
            result.map(function (keyValue) {
                console.log(util.format("key: %s value: %s", keyValue.key(), keyValue.value()));
            });
        } else {
            console.error(util.format("Cannot find data. ERROR: %s", result));
        }
    });
}

exports.findOne = function () {
    var queries = Array.prototype.slice.call(arguments, 0).map(function (arg) {
        return db.query().eq("key", arg);
    });
    var emptyQuery = db.query();
    var baseQuery = queries.shift()
    var q = baseQuery.or.apply(baseQuery, queries);
    return collection.findOne(q).onComplete(function (result, isSuccess) {
        if (isSuccess) {
            console.log(util.format("key: %s value: %s", result.key(), result.value()));
        } else {
            console.error(util.format("Cannot find data. ERROR: %s", result));
        }
    });
}