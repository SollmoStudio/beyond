/* global Query, ObjectId */
var assert = require('assert');
var db = require('db');
var wait = require('test-helper').wait;
require('bdd').mount(this);

var Collection = db.Collection;
var Schema = db.Schema;

function isInstanceOf(obj, constructor) {
  return Object.getPrototypeOf(obj) === constructor.prototype;
}

describe('Schema', function () {
  describe('constructor', function () {
    it('returns a new Schema object.', function () {
      var testSchema1 = new Schema(1, {
        str: { type: 'string' },
        bool: { type: 'boolean' },
        num: { type: 'double' },
        integer: { type: 'int' },
        longInteger: { type: 'long' },
        time: { type: 'date' }
      });
      assert.equal(isInstanceOf(testSchema1, Schema), true);

      var collection1 = new Collection("test.test1", testSchema1);

      var testSchema2 = new Schema(1, {
        username: { type: "string" },
        password: { type: "string" },
        chips: { type: "int", min: 0, max: 999999 },
        profile: { type: "embedding", schema: testSchema1 },
        items: { type: "array", elementType: {
                          "type": "reference", collection: collection1 } },
        characters: { type: "array", elementType: {
                          "type": "embedding", schema: testSchema1 } }
      });
      assert.equal(isInstanceOf(testSchema2, Schema), true);
    });
  });
});

describe('Query', function () {
  var q;
  beforeEach(function () {
    q = db.query();
  });

  describe('#eq()', function () {
    it('returns a query object with a `equal` condition.', function () {
      var query = q.eq('key', 'value');
      assert.equal(isInstanceOf(query, Query), true);
    });
  });

  describe('#neq()', function () {
    it('returns a query object with a `not-equal` condition.', function () {
      var query = q.neq('key', 'value');
      assert.equal(isInstanceOf(query, Query), true);
    });
  });

  describe('#lt()', function () {
    it('returns a query object with a `less-than` condition.', function () {
      var query = q.lt('key', 100);
      assert.equal(isInstanceOf(query, Query), true);
    });
  });

  describe('#gt()', function () {
    it('returns a query object with a `greater-than` condition.', function () {
      var query = q.gt('key', 20);
      assert.equal(isInstanceOf(query, Query), true);
    });
  });

  describe('#lte()', function () {
    it('returns a query object with a `less-than-or-equal` condition.', function () {
      var query = q.lte('key', 100);
      assert.equal(isInstanceOf(query, Query), true);
    });
  });

  describe('#gte()', function () {
    it('returns a query object with a `greater-than-or-equal` condition.', function () {
      var query = q.gte('key', 20);
      assert.equal(isInstanceOf(query, Query), true);
    });
  });

  describe('#where()', function () {
    it('returns a query object with a condition making the callback result true.', function () {
      var query = q.where(function (test) {
        return test.id === 1;
      });
      assert.equal(isInstanceOf(query, Query), true);
    });
  });

  describe('#or()', function () {
    it('returns a query object or-ed with another condition.', function () {
      var q1 = db.query().eq('name', 'kseo');
      var q2 = db.query().eq('name', 'sgkim126');
      var q3 = q1.or(q2);
      assert.equal(isInstanceOf(q3, Query), true);
    });
  });

  describe('#and()', function () {
    it('returns a query object and-ed with another condition.', function () {
      var q1 = db.query().eq('name', 'kseo');
      var q2 = db.query().eq('name', 'sgkim126');
      var q3 = q1.and(q2);
      assert.equal(isInstanceOf(q3, Query), true);
    });
  });
});

describe('ObjectId', function () {
  describe('constructor', function () {
    it('returns a random ObjectId object.', function () {
      var oid = db.ObjectId();
      assert.equal(isInstanceOf(oid, ObjectId), true);
    });

    it('returns a specific ObjectId object.', function () {
      var oid = db.ObjectId("54073619521b5fd89c9cc68a");
      assert.equal(isInstanceOf(oid, ObjectId), true);
    });
  });
});

describe('Collection', function () {
  var schema = new Schema(1, {
    key: { type: 'string' },
    value: { type: 'int' },
    time: { type: 'date' }
  });

  var c = new Collection("test.keyValue", schema);

  beforeEach(function () {
    wait(c.remove(db.query().where(function () { return true; })));
  });

  describe('constructor', function () {
    it('returns a new Schema object.', function () {
      var testSchema1 = new Schema(1, {
        key: { type: 'string' },
        value: { type: 'double' },
        time: { type: 'date' }
      });
      var collection1 = new Collection("test.test1", testSchema1);
      assert.equal(isInstanceOf(collection1, Collection), true);
    });
  });

  describe('#insert()', function () {
    afterEach(function () {
      wait(c.drop());
    });

    it('inserts an object into a collection', function () {
      wait(c.insert({key: 'hello', value: 10, time: new Date()}));

      // check if the object is inserted
      var result = wait(c.find(db.query().eq('key', 'hello')));
      assert.equal(result.length, 1);
      assert.equal(result[0].key(), 'hello');
      assert.equal(result[0].value(), 10);
    });

    it('inserts several objects into a collection', function () {
      var obj1 = {key: 'hello', value: 10, time: new Date()};
      var obj2 = {key: 'world', value: 10, time: new Date()};
      var obj3 = {key: 'three', value: 10, time: new Date()};
      wait(c.insert(obj1, obj2, obj3));

      // check if the object is inserted
      var result = wait(c.find(db.query().eq('value', 10)));
      assert.equal(result.length, 3);
    });
  });

  describe('#find()', function () {
    beforeEach(function () {
      wait(c.insert({key: 'key1', value: 10, time: new Date()}));
      wait(c.insert({key: 'key2', value: 20, time: new Date()}));
      wait(c.insert({key: 'key3', value: 30, time: new Date()}));
      wait(c.insert({key: 'key4', value: 40, time: new Date()}));
      wait(c.insert({key: 'key5', value: 50, time: new Date()}));
    });

    afterEach(function () {
      wait(c.drop());
    });

    it('finds entities meeting a query condition.', function () {
      var q = db.query().gte('value', 20);
      var result = wait(c.find(q));
      assert.equal(result.length, 4);
    });

    it('finds a result with an option.', function () {
      // FIXME: documentation should be updated.
      // https://github.com/SollmoStudio/beyond/wiki/Beyond-DB-API-Collection
    });
  });

  describe('#findOne()', function () {
    beforeEach(function () {
      wait(c.insert({key: 'key1', value: 10, time: new Date()}));
      wait(c.insert({key: 'key2', value: 20, time: new Date()}));
      wait(c.insert({key: 'key3', value: 30, time: new Date()}));
      wait(c.insert({key: 'key4', value: 40, time: new Date()}));
      wait(c.insert({key: 'key5', value: 50, time: new Date()}));
    });

    afterEach(function () {
      wait(c.drop());
    });

    it('finds a single entity meeting a query condition.', function () {
      var q = db.query().eq('key', 'key4');
      var result = wait(c.findOne(q));
      assert.equal(result.key(), 'key4');
      assert.equal(result.value(), 40);
    });
  });

  describe('#remove()', function () {
    beforeEach(function () {
      wait(c.insert({key: 'key1', value: 10, time: new Date()}));
      wait(c.insert({key: 'key2', value: 20, time: new Date()}));
      wait(c.insert({key: 'key3', value: 30, time: new Date()}));
      wait(c.insert({key: 'key4', value: 40, time: new Date()}));
      wait(c.insert({key: 'key5', value: 50, time: new Date()}));
    });

    afterEach(function () {
      wait(c.drop());
    });

    it('removes entities meeting a query condition.', function () {
      var q = db.query().lt('value', 40);
      wait(c.remove(q));

      // check if the entities are removed.
      var q2 = db.query().where(function () { return true; });
      var result = wait(c.find(q2));
      assert.equal(result.length, 2);
    });
  });

  describe('#removeOne()', function () {
    beforeEach(function () {
      wait(c.insert({key: 'key1', value: 10, time: new Date()}));
      wait(c.insert({key: 'key2', value: 20, time: new Date()}));
      wait(c.insert({key: 'key3', value: 30, time: new Date()}));
      wait(c.insert({key: 'key4', value: 40, time: new Date()}));
      wait(c.insert({key: 'key5', value: 50, time: new Date()}));
    });

    afterEach(function () {
      wait(c.drop());
    });

    it('removes a single entity meeting a query condition.', function () {
      var q = db.query().eq('key', 'key2');
      wait(c.remove(q));

      // check if the entity is removed.
      var result = wait(c.find(q));
      assert.equal(result.length, 0);
      var q2 = db.query().where(function () { return true; });
      result = wait(c.find(q2));
      assert.equal(result.length, 4);
    });
  });

  describe('#save()', function () {
    beforeEach(function () {
      wait(c.insert({key: 'hello', value: 10, time: new Date()}));
    });

    afterEach(function () {
      wait(c.drop());
    });

    it('updates a document.', function () {
      var q = db.query().eq('key', 'hello');
      var result = wait(c.findOne(q));
      assert.equal(result.key(), 'hello');
      assert.equal(result.value(), 10);
      result.value(55);
      wait(c.save(result));

      // check if the entity is updated.
      result = wait(c.findOne(q));
      assert.equal(result.key(), 'hello');
      assert.equal(result.value(), 55);
    });
  });

  describe('#drop()', function () {
    beforeEach(function () {
      wait(c.insert({key: 'key1', value: 10, time: new Date()}));
      wait(c.insert({key: 'key2', value: 20, time: new Date()}));
      wait(c.insert({key: 'key3', value: 30, time: new Date()}));
    });

    it('drops a collection successfully.', function () {
      var dropResult = wait(c.drop({}));
      assert.equal(dropResult, true);

      var findResult = wait(c.find(db.query().where(function () { return true; })));
      assert.equal(findResult.length, 0);
      assert.equal(findResult[0], undefined);

      var secondDropResult = wait(c.drop());
      assert.equal(secondDropResult, false);
    });
  });
});
