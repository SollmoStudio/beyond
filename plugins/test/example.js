var assert = require('assert');
require('bdd').mount(this);
require('timer').mount(this);

// Example testcase with JavaScript Array

describe('Array', function () {
  var arr;

  afterEach(function () {
    if (arr) {
      arr = null;
    }
  });

  describe('#indexOf()', function () {
    after(function () {
      arr = null;
    });

    beforeEach(function () {
      arr = [1, 2, 3];
    });

    it('should return the correct index when the value is present', function () {
      assert.equal(0, arr.indexOf(1));
      assert.equal(1, arr.indexOf(2));
      assert.equal(2, arr.indexOf(3));
    });

    it('should return -1 when the value is not present', function () {
      assert.equal(-1, arr.indexOf(5));
      assert.equal(-1, arr.indexOf(0));
    });
  });

  describe('#map()', function () {
    beforeEach(function () {
      arr = [1, 2, 3];
    });

    it('should return an array mapped by the mapping function', function () {
      assert.deepEqual([2, 3, 4], arr.map(function (el) { return el + 1; }));
    });
  });
});

describe('Array (async)', function () {
  var arr;

  afterEach(function () {
    if (arr) {
      arr = null;
    }
  });

  describe('#indexOf()', function () {
    after(function () {
      arr = null;
    });

    beforeEach(function () {
      arr = [1, 2, 3];
    });

    it.will('return the correct index when the value is present', function (done) {
      try {
        assert.equal(0, arr.indexOf(1));
        assert.equal(1, arr.indexOf(2));
        assert.equal(2, arr.indexOf(3));
        done();
      } catch (e) {
        done(e);
      }
    });

    it.will('return -1 when the value is not present', function (done) {
      try {
        assert.equal(-1, arr.indexOf(5));
        assert.equal(-1, arr.indexOf(0));
        done();
      } catch (e) {
        done(e);
      }
    });
  });

  describe('#map()', function () {
    beforeEach(function () {
      arr = [1, 2, 3];
    });

    it.will('return an array mapped by the mapping function', function (done) {
      try {
        assert.deepEqual([2, 3, 4], arr.map(function (el) { return el + 1; }));
        done();
      } catch (e) {
        done(e);
      }
    });
  });
});
