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

function exec(command) {
  return java.lang.Runtime.getRuntime().exec(command);
}

before(function () {
  exec('./plugins/test/assets/http-client/run-test-server.sh').waitFor();
});

after(function () {
  exec('curl http://localhost:1234/quit');
});

describe('http-client', function () {
  describe('#get()', function () {
    var req;
    beforeEach(function () {
      req = request.get('http://localhost:1234/get?hello=world');
    });

    it.will('send a get request.', function (done) {
      req
        .end()
        .onComplete(complete(done, function (res) {
          assert.async(done, function () {
            var result = JSON.parse(res.body);
            assert.equal(result.url, '/get?hello=world');
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
        .set({'hello': 'world', 'beyond': 'framework', 'number': 1234})
        .end()
        .onComplete(complete(done, function (res) {
          assert.async(done, function () {
            var result = JSON.parse(res.body);
            assert.equal(result.headers.hello, 'world');
            assert.equal(result.headers.beyond, 'framework');
            assert.equal(result.headers.number, '1234');
          });
        }));
    });
  });

  describe('#post()', function () {
    var req;
    beforeEach(function () {
      req = request.post('http://localhost:1234/post');
    });

    it.will('send a post request.', function (done) {
      req
        .end()
        .onComplete(complete(done, function (res) {
          assert.async(done, function () {
            var result = JSON.parse(res.body);
            assert.equal(result.url, '/post');
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
        .set({'hello': 'world', 'beyond': 'framework', 'number': 1234})
        .end()
        .onComplete(complete(done, function (res) {
          assert.async(done, function () {
            var result = JSON.parse(res.body);
            assert.equal(result.headers.hello, 'world');
            assert.equal(result.headers.beyond, 'framework');
            assert.equal(result.headers.number, '1234');
          });
        }));
    });
  });

  describe('#put()', function () {
    var req;
    beforeEach(function () {
      req = request.put('http://localhost:1234/put');
    });

    it.will('send a put request.', function (done) {
      req
        .end()
        .onComplete(complete(done, function (res) {
          assert.async(done, function () {
            var result = JSON.parse(res.body);
            assert.equal(result.url, '/put');
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
        .set({'hello': 'world', 'beyond': 'framework', 'number': 1234})
        .end()
        .onComplete(complete(done, function (res) {
          assert.async(done, function () {
            var result = JSON.parse(res.body);
            assert.equal(result.headers.hello, 'world');
            assert.equal(result.headers.beyond, 'framework');
            assert.equal(result.headers.number, '1234');
          });
        }));
    });
  });

  describe('#auth()', function () {
    it.will('send a request with basic auth.', function (done) {
      request
        .get('http://localhost:1234/basic-auth')
        .query({'id': 'hello', 'passwd': 'world'})
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
