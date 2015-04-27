var console = require('console');
var db = require('db');
var util = require('util');

var schema = new db.Schema(1, {
  key: { type: 'string' },
  value: { type: 'double' },
  time: { type: 'date' }
});
var collection = new db.Collection("example.keyValue", schema);

var schema2 = new db.Schema(1, {
  key: { type: 'string' },
  value: { type: 'double' },
  time: { type: 'date' },
  ref: { type: 'reference', collection: collection }
});
var collection2 = new db.Collection('example.reference', schema2);

var embeddingSchema = new db.Schema(1, {
  embed: { type: 'embedding', schema: schema }
});
var embeddingCollection = new db.Collection('example.embedding', embeddingSchema);

var arraySchema = new db.Schema(1, {
  intArray: { type: 'array', elementType: { type: 'int' } },
  embedArray: { type: 'array', elementType: { type: 'embedding', schema: schema } },
  refArray: { type: 'array', elementType: { type: 'reference', collection: collection } }
});
var arrayCollection = new db.Collection("example.array", arraySchema);

exports.insert = function (key, value) {
  return collection.insert({key: key, value: value, time: new Date()})
    .onFailure(console.error)
    .onSuccess(function (doc) {
      console.info(
        "New document{ _id: %s, key: %s, value: %s, time: %s } is inserted.",
        doc.objectId.stringify, doc.key(), doc.value(), doc.time());
    });
};

exports.find = function () {
  var queries = Array.prototype.slice.call(arguments, 0).map(function (arg) {
    return db.query("key", arg);
  });
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
    return db.query("key", arg);
  });
  var baseQuery = queries.shift();
  var q = baseQuery.or.apply(baseQuery, queries);
  return collection.findOne(q).onComplete(function (result, isSuccess) {
    if (isSuccess) {
      if (result) {
        console.log(util.format("key: %s value: %s time: %s", result.key(), result.value()), result.time());
      } else {
        console.log("Cannot find data");
      }
    } else {
      console.error(util.format("Cannot find data. ERROR: %s", result));
    }
  });
};

exports.remove = function () {
  var queries = Array.prototype.slice.call(arguments, 0).map(function (arg) {
    return db.query("key", arg);
  });
  var baseQuery = queries.shift();
  var q = baseQuery.or.apply(baseQuery, queries);
  return collection.remove(q).onSuccess(console.info).onFailure(console.error);
};

exports.removeOne = function () {
  var queries = Array.prototype.slice.call(arguments, 0).map(function (arg) {
    return db.query("key", arg);
  });
  var baseQuery = queries.shift();
  var q = baseQuery.or.apply(baseQuery, queries);
  return collection.removeOne(q).onSuccess(console.info).onFailure(console.error);
};

exports.drop = function () {
  return collection.drop().onSuccess(console.info).onFailure(console.error);
};

exports.save = function () {
  var queries = Array.prototype.slice.call(arguments, 1).map(function (arg) {
    return db.query("key", arg);
  });
  var newValue = arguments[0];
  var baseQuery = queries.shift();
  var q = baseQuery.or.apply(baseQuery, queries);
  return collection.findOne(q).onComplete(function (result, isSuccess) {
    if (isSuccess) {
      console.log(util.format("key: %s value: %s time: %s", result.key(), result.value()), result.time());
    } else {
      console.error(util.format("Cannot find data. ERROR: %s", result));
    }
  }).flatMap(function (result) {
    return collection.save(result.value(newValue).time(new Date())).onComplete(console.log).onSuccess(function (doc) {
      console.log("%j", doc);
    });
  });
};

exports.findOneWithKey = function (key) {
  var query = db.query("_id", db.ObjectId(key));
  return collection.findOne(query).onComplete(function (result, isSuccess) {
    if (isSuccess) {
      console.log("%j", result);
    } else {
      console.error("Cannot find data. ERROR: %s", result);
    }
  });
};

exports.referenceInsert = function (key) {
  var query = db.query("key", key);
  return collection.findOne(query).onSuccess(function (result) {
    console.log("document1: %j", result);
  }).onFailure(function (message) {
    console.error("Cannot find data. ERROR: %s", message);
  }).map(function (data) {
    console.log("ID: %s", data.objectId.stringify);
    if (data) {
      return collection2.insert({ key: data.key(), value: data.value(), time: data.time(), ref: data }).onComplete(console.error);
    }
    return null;
  });
};

exports.referenceFindOne = function (key) {
  var query = db.query("key", key);
  return collection2.findOne(query).flatMap(function (result) {
    var query = db.query("_id", result.ref());
    return collection.findOne(query);
  }).onSuccess(function (result) {
    console.log("Found data: %j", result);
  }).onFailure(function (message) {
    console.error("Cannot find data. ERROR: %s", message);
  });
};

exports.referenceUpdate = function (key1, key2) {
  var query1 = db.query("key", key1);
  var query2 = db.query("key", key2);

  return collection.findOne(query1).onSuccess(function (result) {
    console.log("document1: %j", result);
  }).onFailure(function (message) {
    console.error("Cannot find document1: %s", message);
  }).flatMap(function (document1) {
    return collection2.findOne(query2).onSuccess(function (result) {
      console.log("document2: %j", result);
    }).onFailure(function (message) {
      console.error("Cannot find document2: %s", message);
    }).flatMap(function (document2) {
      var query = db.query("_id", document2.ref());
      return collection.findOne(query).onSuccess(function (result) {
        console.log("document2.ref: %j", result);
      }).onFailure(function (message) {
        console.error("Cannot find documennt2.ref: %s", message);
      }).flatMap(function () {
        return collection2.save(document2.ref(document1));
      });
    });
  });
};

exports.embeddingInsert = function (key, value) {
  embeddingCollection.insert({ embed: {
    key: key,
    value: value,
    time: new Date()
  }}).onFailure(console.error).onSuccess(function (doc) {
    console.log("doc: %j", doc);
  });
};

exports.embeddingFind = function () {
  var queries = Array.prototype.slice.call(arguments, 0).map(function (arg) {
    return db.query("embed.key", arg);
  });
  var baseQuery = queries.shift();
  var query = baseQuery.or.apply(baseQuery, queries);
  embeddingCollection.find(query).onFailure(console.error).onSuccess(function (result) {
    console.log(util.format("Find %d keyValue", result.length));
    result.map(function (result) {
      console.log(": %j", result);
    });
  });
};

exports.arrayInsert = function(refKey, key1, value1, key2, value2) {
  var values = Array.prototype.slice.call(arguments, 5);
  var refQuery = db.query('key', refKey);
  return collection.find(refQuery).onComplete(console.log).flatMap(function (refDocs) {
    var e1 = { key: key1, value: value1 };
    var e2 = { key: key2, value: value2 };
    return arrayCollection
      .insert({ intArray: values, refArray: refDocs, embedArray: [ e1, e2 ] })
      .onComplete(console.log);
  }).onComplete(console.log);
};

exports.arrayFindOne = function(key) {
  var query = db.query('_id', db.ObjectId(key));
  return arrayCollection.findOne(query).onComplete(console.log).onSuccess(function (doc) {
    console.log("%j", doc);
  });
};

exports.count = function () {
  var queries = Array.prototype.slice.call(arguments, 0).map(function (arg) {
    return db.query("key", arg);
  });
  var baseQuery = queries.shift();
  var q = baseQuery.or.apply(baseQuery, queries);
  return collection.count(q).onComplete(console.log);
};
