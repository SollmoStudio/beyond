var assert = require('assert');
var crypto = require('crypto');
require('bdd').mount(this);

describe('crypto', function () {
  describe('#signToken()', function () {
    it('returns a signed value of a token.', function () {
      var signedTokenRegex = /^[a-f0-9]{40}-[0-9]{13}-token-string$/;
      assert.equal(!!signedTokenRegex.exec(crypto.signToken('token-string')), true);
    });
  });

  describe('#sign()', function () {
    it('returns a signed value with HMAC-SHA1.', function () {
      var sign1 = crypto.sign('Hello, world!');
      var sign2 = crypto.sign('Hello, world!');
      var signRegex = /^[a-f0-9]{40}$/;
      assert.equal(sign1, sign2);
    });

    it('returns a signed value using a string-typed key.', function () {
      var sign = crypto.sign('Hello, world!', 'key-string');
      assert.equal(sign, 'bad1e06155db2050d18a42fcc763d94ba79c5c38');
    });

    it('returns a signed value using a integer-array key.', function () {
      var key = [107, 101, 121, 45, 115, 116, 114, 105, 110, 103]; // 'key-string'
      var sign = crypto.sign('Hello, world!', key);
      assert.equal(sign, 'bad1e06155db2050d18a42fcc763d94ba79c5c38');
    });
  });

  describe('#generateToken()', function () {
    it('returns a generated token.', function () {
      var tokenRegex = /^[a-f0-9]{24}$/;
      assert.equal(!!tokenRegex.exec(crypto.generateToken()), true);
    });
  });

  describe('#generateSignedToken()', function () {
    it('returns a generated token.', function () {
      var signedTokenRegex = /^[a-f0-9]{40}-[0-9]{13}-[a-f0-9]{24}$/;
      assert.equal(!!signedTokenRegex.exec(crypto.generateSignedToken()), true);
    });
  });

  describe('#extractSignedToken()', function () {
    it('returns the original token of a signed token.', function () {
      var token = crypto.generateToken();
      var signedToken = crypto.signToken(token);
      assert.equal(crypto.extractSignedToken(signedToken), token);
    });
  });

  describe('#compareSignedTokens()', function () {
    it('returns true if original tokens are the same.', function () {
      var signedToken1 = crypto.signToken('token-string');
      var signedToken2 = crypto.signToken('token-string');
      assert.equal(crypto.compareSignedTokens(signedToken1, signedToken2), true);
    });

    it('returns false if original tokens are different.', function () {
      var signedToken1 = crypto.signToken('token-string');
      var signedToken2 = crypto.signToken('diffrent-str');
      assert.equal(crypto.compareSignedTokens(signedToken1, signedToken2), false);
    });
  });

  describe('#encryptAES()', function () {
    it('returns an encrypted value with AES.', function () {
      var encryptedValueRegex = /^[a-f0-9]+$/;
      assert.equal(!!encryptedValueRegex.exec(crypto.encryptAES('Hello, encryption!')), true);
    });

    it('returns an encrypted value with AES using a key.', function () {
      var encryptedValue = crypto.encryptAES('Hello encryption! It is 32 bits.', 'some-private-key');
      var expected = '778a6d3ee2698c57c441daad2c44145e09300181d897ff4aa32acc038227b62ea41c17d56f527056edcb3c495b13b02b';
      assert.equal(encryptedValue, expected);
    });
  });

  describe('#decrypteAES()', function () {
    it('returns the original value of an AES-encrypted value.', function () {
      var originalText = 'Hello encryption! It is 32 bits.';
      var encryptedText = crypto.encryptAES(originalText);
      assert.equal(crypto.decryptAES(encryptedText), originalText);
    });

    it('returns the original value of an AES-encrypted value with a key.', function () {
      var originalText = 'Hello encryption! It is 32 bits.';
      var encryptedText = crypto.encryptAES(originalText, 'some-private-key');
      assert.equal(crypto.decryptAES(encryptedText, 'some-private-key'), originalText);
    });
  });

  describe('#constantTimeEquals()', function () {
    it('compares two strings with the same length in constant time.', function () {
      assert.equal(crypto.constantTimeEquals('Beyond Framework', 'Beyond Framework'), true);
      assert.equal(crypto.constantTimeEquals('Beyond Framework', 'Beyond Framework in Scala'), false);
      assert.equal(crypto.constantTimeEquals('Beyond Framework', 'Within Framework'), false);
    });
  });
});
