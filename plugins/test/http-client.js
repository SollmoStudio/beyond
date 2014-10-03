var assert = require('assert');
var request = require('http-client');
require('bdd').mount(this);

function complete(done, callback) {
  return function (res, success) {
    if (success) {
      callback(res);
    } else {
      done('failed.');
    }
  };
}

describe('http-client', function () {
  describe('#get()', function () {
    var req;
    beforeEach(function () {
      req = request.get('http://httpbin.org/get?hello=world');
    });

    it.will('send a get request.', function (done) {
      req
        .end()
        .onComplete(complete(done, function (res) {
          assert.async(done, function () {
            var result = JSON.parse(res.body);
            assert.equal(result.url, 'http://httpbin.org/get?hello=world');
          });
        }));
    });

    it.will('send a get request with query string in url.', function (done) {
      req
        .end()
        .onComplete(complete(done, function (res) {
          assert.async(done, function () {
            var result = JSON.parse(res.body);
            assert.equal(result.args.hello, 'world');
          });
        }));
    });

    it.will('send a get request with query string with query().', function (done) {
      req
        .query({'query': 'string'})
        .end()
        .onComplete(complete(done, function (res) {
          assert.async(done, function () {
            var result = JSON.parse(res.body);
            assert.equal(result.args.hello, 'world');
            assert.equal(result.args.query, 'string');
          });
        }));
    });

    it.will('send a get request with headers.', function (done) {
      req
        .set({'Hello': 'world', 'Beyond': 'framework', 'Number': 1234})
        .end()
        .onComplete(complete(done, function (res) {
          assert.async(done, function () {
            var result = JSON.parse(res.body);
            assert.equal(result.headers.Hello, 'world');
            assert.equal(result.headers.Beyond, 'framework');
            assert.equal(result.headers.Number, '1234');
          });
        }));
    });
  });

  describe('#post()', function () {
    var req;
    beforeEach(function () {
      req = request.post('http://httpbin.org/post');
    });

    it.will('send a post request.', function (done) {
      req
        .end()
        .onComplete(complete(done, function (res) {
          assert.async(done, function () {
            var result = JSON.parse(res.body);
            assert.equal(result.url, 'http://httpbin.org/post');
          });
        }));
    });

    it.will('send a post request with form data.', function (done) {
      req
        .send({'hello': 'world', 'beyond': 'framework'})
        .end()
        .onComplete(complete(done, function (res) {
          assert.async(done, function () {
            var result = JSON.parse(res.body);
            assert.equal(result.form.hello, 'world');
            assert.equal(result.form.beyond, 'framework');
          });
        }));
    });

    it.will('send a post request with JSON data.', function (done) {
      req
        .json({'hello': 'world', 'beyond': 'framework'})
        .end()
        .onComplete(complete(done, function (res) {
          assert.async(done, function () {
            var result = JSON.parse(res.body);
            var jsonData = JSON.parse(result.data);
            assert.equal(jsonData.hello, 'world');
            assert.equal(jsonData.beyond, 'framework');
          });
        }));
    });

    it.will('send a post request with headers.', function (done) {
      req
        .set({'Hello': 'world', 'Beyond': 'framework', 'Number': 1234})
        .end()
        .onComplete(complete(done, function (res) {
          assert.async(done, function () {
            var result = JSON.parse(res.body);
            assert.equal(result.headers.Hello, 'world');
            assert.equal(result.headers.Beyond, 'framework');
            assert.equal(result.headers.Number, '1234');
          });
        }));
    });
  });

  describe('#put()', function () {
    var req;
    beforeEach(function () {
      req = request.put('http://httpbin.org/put');
    });

    it.will('send a put request.', function (done) {
      req
        .end()
        .onComplete(complete(done, function (res) {
          assert.async(done, function () {
            var result = JSON.parse(res.body);
            assert.equal(result.url, 'http://httpbin.org/put');
          });
        }));
    });

    it.will('send a put request with form data.', function (done) {
      req
        .send({'hello': 'world', 'beyond': 'framework'})
        .end()
        .onComplete(complete(done, function (res) {
          assert.async(done, function () {
            var result = JSON.parse(res.body);
            assert.equal(result.form.hello, 'world');
            assert.equal(result.form.beyond, 'framework');
          });
        }));
    });

    it.will('send a put request with JSON data.', function (done) {
      req
        .json({'hello': 'world', 'beyond': 'framework'})
        .end()
        .onComplete(complete(done, function (res) {
          assert.async(done, function () {
            var result = JSON.parse(res.body);
            var jsonData = JSON.parse(result.data);
            assert.equal(jsonData.hello, 'world');
            assert.equal(jsonData.beyond, 'framework');
          });
        }));
    });

    it.will('send a put request with headers.', function (done) {
      req
        .set({'Hello': 'world', 'Beyond': 'framework', 'Number': 1234})
        .end()
        .onComplete(complete(done, function (res) {
          assert.async(done, function () {
            var result = JSON.parse(res.body);
            assert.equal(result.headers.Hello, 'world');
            assert.equal(result.headers.Beyond, 'framework');
            assert.equal(result.headers.Number, '1234');
          });
        }));
    });
  });

  describe('#auth()', function () {
    it.will('send a request with basic auth.', function (done) {
      request
        .get('http://httpbin.org/basic-auth/hello/world')
        .auth('hello', 'world')
        .end()
        .onComplete(complete(done, function (res) {
          assert.async(done, function () {
            var result = JSON.parse(res.body);
            assert.equal(result.authenticated, true);
          });
        }));
    });
  });
});
