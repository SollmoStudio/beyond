var assert = require('assert');
var Response = require("response").Response;
require('bdd').mount(this);

describe('Response', function () {
  describe('#constructor()', function () {
    it('should return a proper Response object.', function () {
      var res = new Response('hello');
      assert.equal(!!res, true);
    });

    it('should return an object with a JSON body.', function () {
      var res = new Response({hello: 'world'});
      assert.equal(!!res, true);
    });

    it('should return an object with a content type.', function () {
      var res = new Response('hello', 'image/jpeg');
      assert.equal(!!res, true);
    });

    it('should return an object with a content type and status code.', function () {
      var res = new Response('hello', 'image/jpeg', 301);
      assert.equal(!!res, true);
    });
  });

  describe('#body', function () {
    it('should return the body string of a Response obj.', function () {
      var res = new Response('hello');
      assert.equal(res.body, 'hello');
    });

    it('should return the stringified JSON body of a Response obj.', function () {
      var body = {hello: 'world!'};
      var res = new Response(body);
      assert.equal(res.body, JSON.stringify(body));
    });
  });

  describe('#contentType', function () {
    it('should return the content type of a Response obj.', function () {
      var res = new Response('hello', 'image/jpeg');
      assert.equal(res.contentType, 'image/jpeg');
    });
  });

  describe('#statusCode', function () {
    it('should return the status code of a Response obj.', function () {
      var res = new Response('hello', 'image/jpeg', 301);
      assert.equal(res.statusCode, 301);
    });
  });
});
