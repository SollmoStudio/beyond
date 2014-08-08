var console = require('console');
var db = require('db');
var util = require('util');

var schema = new db.Schema(1, {
    key: { type: 'string' },
    value: { type: 'double' },
    time: { type: 'date' }
});
var collection = new db.Collection("example.keyValue", schema);

exports.insert = function (key, value) {
    return collection.insert({key: key, value: value, time: new Date()})
        .onFailure(console.error)
        .onSuccess(function (doc) {
            console.info(
                "New document{ _id: %s, key: %s, value: %s, time: %s } is inserted.",
                doc.objectID, doc.key(), doc.value(), doc.time());
        });
};

exports.find = function () {
    var queries = Array.prototype.slice.call(arguments, 0).map(function (arg) {
        return db.query().eq("key", arg);
    });
    var emptyQuery = db.query();
    var baseQuery = queries.shift();
    var q = baseQuery.or.apply(baseQuery, queries);
    return collection.find(q).onComplete(function (result, isSuccess) {
        if (isSuccess) {
            console.log(util.format("Find %d keyValue", result.length));
            result.map(function (keyValue) {
                console.log(util.format("key: %s value: %s time: %s", keyValue.key(), keyValue.value()), keyValue.time());
            });
        } else {
            console.error(util.format("Cannot find data. ERROR: %s", result));
        }
    });
};

exports.findOne = function () {
    var queries = Array.prototype.slice.call(arguments, 0).map(function (arg) {
        return db.query().eq("key", arg);
    });
    var emptyQuery = db.query();
    var baseQuery = queries.shift();
    var q = baseQuery.or.apply(baseQuery, queries);
    return collection.findOne(q).onComplete(function (result, isSuccess) {
        if (isSuccess) {
            console.log(util.format("key: %s value: %s time: %s", result.key(), result.value()), result.time());
        } else {
            console.error(util.format("Cannot find data. ERROR: %s", result));
        }
    });
};

exports.remove = function () {
    var queries = Array.prototype.slice.call(arguments, 0).map(function (arg) {
        return db.query().eq("key", arg);
    });
    var emptyQuery = db.query();
    var baseQuery = queries.shift();
    var q = baseQuery.or.apply(baseQuery, queries);
    return collection.remove(q).onSuccess(console.info).onFailure(console.error);
};

exports.removeOne = function () {
    var queries = Array.prototype.slice.call(arguments, 0).map(function (arg) {
        return db.query().eq("key", arg);
    });
    var emptyQuery = db.query();
    var baseQuery = queries.shift();
    var q = baseQuery.or.apply(baseQuery, queries);
    return collection.removeOne(q).onSuccess(console.info).onFailure(console.error);
};

exports.save = function () {
    var queries = Array.prototype.slice.call(arguments, 1).map(function (arg) {
        return db.query().eq("key", arg);
    });
    var newValue = arguments[0];
    var emptyQuery = db.query();
    var baseQuery = queries.shift();
    var q = baseQuery.or.apply(baseQuery, queries);
    return collection.findOne(q).onComplete(function (result, isSuccess) {
        if (isSuccess) {
            console.log(util.format("key: %s value: %s time: %s", result.key(), result.value()), result.time());
        } else {
            console.error(util.format("Cannot find data. ERROR: %s", result));
        }
    }).flatMap(function (result) {
        return collection.save(result.value(newValue).time(new Date())).onComplete(console.log);
    });
};
