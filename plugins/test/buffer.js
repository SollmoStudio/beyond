var assert = require('assert');
var Buffer = require('buffer').Buffer;
require('bdd').mount(this);

describe('Buffer', function () {
  describe('#constructor()', function () {
    it('constructs a new buffer with a size.', function () {
      var buffer = new Buffer(10);
      assert.deepEqual(buffer.toJSON(), [0, 0, 0, 0, 0, 0, 0, 0, 0, 0]);
    });

    it('constructs a new buffer with a byte array.', function () {
      var buffer = new Buffer([0x01, 0x30, 0x05, 0x70, 0x09, 0xa0, 0x0c, 0xe0, 0x0f]);
      assert.deepEqual(buffer.toJSON(), [1, 48, 5, 112, 9, 160, 12, 224, 15]);
    });

    it('constructs a new buffer with a string.', function () {
      var buffer = new Buffer('Hello, world!');
      assert.deepEqual(buffer.toJSON(), [72, 101, 108, 108, 111, 44, 32, 119, 111, 114, 108, 100, 33]);
    });

    it('constructs a new buffer with a string with an encoding.', function () {
      var buffer1 = new Buffer('안녕, 세계!', 'UTF-8');
      assert.deepEqual(buffer1.toJSON(), [236, 149, 136, 235, 133, 149, 44, 32, 236, 132, 184, 234, 179, 132, 33]);
      var buffer2 = new Buffer('안녕, 세계!', 'EUC-KR');
      assert.deepEqual(buffer2.toJSON(), [190, 200, 179, 231, 44, 32, 188, 188, 176, 232, 33]);
    });
  });

  describe('#isSupportedEncoding()', function () {
    it('returns true if an encoding is supported', function () {
      assert.equal(Buffer.isSupportedEncoding('ASCII'), true);
      assert.equal(Buffer.isSupportedEncoding('UTF-8'), true);
      assert.equal(Buffer.isSupportedEncoding('EUC-KR'), true);
      assert.equal(Buffer.isSupportedEncoding('CP949'), true);
    });

    it('returns false if an encoding is not supported.', function () {
      assert.equal(Buffer.isSupportedEncoding('ASCI'), false);
      assert.equal(Buffer.isSupportedEncoding('UTF-9'), false);
      assert.equal(Buffer.isSupportedEncoding('EUC-RK'), false);
      assert.equal(Buffer.isSupportedEncoding('PC949'), false);
    });
  });

  describe('#toString()', function () {
    it('returns a string representing the buffer.', function () {
      var buffer = new Buffer('Beyond Framework');
      assert.equal(buffer.toString(), 'Beyond Framework');
    });

    it('returns a string representing the buffer with an encoding.', function () {
      var buffer1 = new Buffer('안녕, 세계!');
      assert.equal(buffer1.toString('UTF-8'), '안녕, 세계!');
      var buffer2 = new Buffer([190, 200, 179, 231, 44, 32, 188, 188, 176, 232, 33]);
      assert.equal(buffer2.toString('EUC-KR'), '안녕, 세계!');
    });

    it('returns a string representing the buffer with an encoding and an offset.', function () {
      var buffer1 = new Buffer('안녕, 세계!');
      assert.equal(buffer1.toString('UTF-8', 8), '세계!');
      var buffer2 = new Buffer([190, 200, 179, 231, 44, 32, 188, 188, 176, 232, 33]);
      assert.equal(buffer2.toString('EUC-KR', 6), '세계!');
    });

    it('returns a string representing the buffer with an encoding, start and end offsets.', function () {
      var buffer1 = new Buffer('안녕, 세계!');
      assert.equal(buffer1.toString('UTF-8', 8, 11), '세');
      var buffer2 = new Buffer([190, 200, 179, 231, 44, 32, 188, 188, 176, 232, 33]);
      assert.equal(buffer2.toString('EUC-KR', 6, 10), '세계');
    });
  });

  describe('#write()', function () {
    it('writes a string to a buffer.', function () {
      var buffer = new Buffer(128);
      buffer.write('안녕, 세계!');
      assert.equal(buffer.toString('UTF-8', 0, 15), '안녕, 세계!');
    });

    it('writes a string to a buffer with an offset.', function () {
      var buffer = new Buffer(128);
      var byteLength;
      byteLength = buffer.write('녕, 세', 3);
      assert.equal(byteLength, 8);
      byteLength = buffer.write('계!', 11);
      assert.equal(byteLength, 4);
      byteLength = buffer.write('안', 0);
      assert.equal(byteLength, 3);
      assert.equal(buffer.toString('UTF-8', 0, 15), '안녕, 세계!');
    });

    it('writes a string to a buffer with an offset and a length.', function () {
      var buffer = new Buffer(128);
      var byteLength;
      byteLength = buffer.write('녕, 세', 3, 5);
      assert.equal(byteLength, 5);
      byteLength = buffer.write('계!', 8, 4);
      assert.equal(byteLength, 4);
      byteLength = buffer.write('안', 0, 3);
      assert.equal(byteLength, 3);
      assert.equal(buffer.toString('UTF-8', 0, 12), '안녕, 계!');
    });

    it('writes a string to a buffer with an offset, a length and an encoding.', function () {
      var buffer = new Buffer(128);
      var byteLength;
      byteLength = buffer.write('녕, 세', 2, 3, 'EUC-KR');
      assert.equal(byteLength, 3);
      byteLength = buffer.write('계!', 5, 3, 'EUC-KR');
      assert.equal(byteLength, 3);
      byteLength = buffer.write('안', 0, 2, 'EUC-KR');
      assert.equal(byteLength, 2);
      assert.equal(buffer.toString('EUC-KR', 0, 8), '안녕,계!');
    });
  });

  describe('#toJSON()', function () {
    it('returns a JSON object of the provided buffer.', function () {
      var array = new Array(255);
      for (var i = 0; i < 255; i++) {
        array[i] = i;
      }
      var buffer = new Buffer(array);
      assert.deepEqual(buffer.toJSON(), array);
    });
  });

  describe('#byteLength()', function () {
    it('returns a byte length of the provided string.', function () {
      assert.equal(Buffer.byteLength('Hello, world!'), 13);
    });

    it('returns a byte length of the provided string with an encoding.', function () {
      assert.equal(Buffer.byteLength('안녕, 세계!', 'UTF-8'), 15);
      assert.equal(Buffer.byteLength('안녕, 세계!', 'EUC-KR'), 11);
    });
  });

  describe('#concat()', function () {
    var buf1, buf2, buf3, buf4, buf5;
    beforeEach(function () {
      buf1 = new Buffer('Hel');
      buf2 = new Buffer('lo, ');
      buf3 = new Buffer('world');
      buf4 = new Buffer('!abcde');
      buf5 = new Buffer('fghijkl');
    });

    it('concatenates the provided buffers and returns the result.', function () {
      assert.equal(Buffer.concat([buf1, buf2, buf3, buf4, buf5]), 'Hello, world!abcdefghijkl');
    });

    it('concatenates the provided buffers with a total length and returns the result.', function () {
      assert.equal(Buffer.concat([buf1, buf2, buf3, buf4, buf5], 13), 'Hello, world!');
    });
  });

  describe('#fill()', function () {
    var buffer;
    beforeEach(function () {
      buffer = new Buffer(10);
    });

    it('fills the provided buffer with a value.', function () {
      buffer.fill(15);
      assert.deepEqual(buffer.toJSON(), [15, 15, 15, 15, 15, 15, 15, 15, 15, 15]);
    });

    it('fills the provided buffer with a value and an offset.', function () {
      buffer.fill(15);
      buffer.fill(3, 7);
      assert.deepEqual(buffer.toJSON(), [15, 15, 15, 15, 15, 15, 15, 3, 3, 3]);
    });

    it('fills the provided buffer with a value, start and end offsets.', function () {
      buffer.fill(15);
      buffer.fill(3, 7);
      buffer.fill(6, 1, 5);
      assert.deepEqual(buffer.toJSON(), [15, 6, 6, 6, 6, 15, 15, 3, 3, 3]);
    });
  });

  describe('#copyTo()', function () {
    var source, target;
    beforeEach(function () {
      source = new Buffer([1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);
      target = new Buffer(15);
      target.fill(11);
    });

    it('copies a buffer to another buffer.', function () {
      source.copyTo(target);
      assert.deepEqual(target.toJSON(), [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 11, 11, 11, 11]);
    });

    it('copies a buffer with a target offset.', function () {
      source.copyTo(target, 2);
      assert.deepEqual(target.toJSON(), [11, 11, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 11, 11]);
    });

    it('copies a buffer with a target offset and a source offset.', function () {
      source.copyTo(target, 2, 3);
      assert.deepEqual(target.toJSON(), [11, 11, 4, 5, 6, 7, 8, 9, 10, 11, 11, 11, 11, 11, 11]);
    });

    it('copies a buffer with a target offset and source start and end offsets.', function () {
      source.copyTo(target, 2, 3, 7);
      assert.deepEqual(target.toJSON(), [11, 11, 4, 5, 6, 7, 11, 11, 11, 11, 11, 11, 11, 11, 11]);
    });
  });

  describe('#slice()', function () {
    var original;
    beforeEach(function () {
      original = new Buffer([1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);
    });

    it('slices a buffer and returns the result.', function () {
      var buffer = original.slice();
      assert.deepEqual(buffer.toJSON(), [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);
    });

    it('slices a buffer with an offset.', function () {
      var buffer = original.slice(3);
      assert.deepEqual(buffer.toJSON(), [4, 5, 6, 7, 8, 9, 10]);
    });

    it('slices a buffer with start and end offsets.', function () {
      var buffer = original.slice(3, 8);
      assert.deepEqual(buffer.toJSON(), [4, 5, 6, 7, 8]);
    });
  });

  describe('#length', function () {
    it('returns the length of a buffer.', function () {
      var buffer1 = new Buffer(10);
      assert.equal(buffer1.length, 10);
      var buffer2 = new Buffer([0x01, 0x30, 0x05, 0x70, 0x09, 0xa0, 0x0c, 0xe0, 0x0f]);
      assert.equal(buffer2.length, 9);
      var buffer3 = new Buffer('Hello, world!');
      assert.equal(buffer3.length, 13);
    });
  });
});
