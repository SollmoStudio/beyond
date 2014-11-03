var assert = require('assert');
var uuid = require('uuid');
require('bdd').mount(this);

describe('uuid', function () {
  describe('#v1()', function () {
    it('returns a time-based uuid.', function () {
      var id = uuid.v1();
      var idRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
      assert.equal(!!idRegex.exec(id), true);
    });

    it('returns a uuid generated with a node.', function () {
      var id = uuid.v1({node: 0x0123456789ab});
      var idRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-0123456789ab$/;
      assert.equal(!!idRegex.exec(id), true);
    });

    it('returns a uuid generated with time options.', function () {
      var id = uuid.v1({
        clockseq: 0x1234,
        msecs: new Date('2014-01-01').getTime(),
        nsecs: 5678
      });
      var idRegex = /^1381562e-dd21-1b21-9234-[0-9a-f]{12}$/;
      assert.equal(!!idRegex.exec(id), true);
    });

    it('returns a uuid generated with all options.', function () {
      var id = uuid.v1({
        node: 0x0123456789ab,
        clockseq: 0x1234,
        msecs: new Date('2014-01-01').getTime(),
        nsecs: 5678
      });
      var expected = '1381562e-dd21-1b21-9234-0123456789ab';
      assert.equal(id, expected);
    });
  });

  describe('#v4()', function () {
    it('returns a randomized uuid.', function () {
      var id = uuid.v4();
      var idRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
      assert.equal(!!idRegex.exec(id), true);
    });
  });
});
