var assert = require('assert');
var request = require('http-client');
var wait = require('test-helper').wait;
require('bdd').mount(this);

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

    it('send a get request.', function () {
      var res = wait(req.end());
      var result = JSON.parse(res.body);
      assert.equal(result.url, '/get?hello=world');
    });

    it('send a get request with query string in url.', function () {
      var res = wait(req.end());
      var result = JSON.parse(res.body);
      assert.equal(result.args.hello, 'world');
    });

    it('send a get request with query string with query().', function () {
      var res = wait(req.query({'query': 'string'}).end());
      var result = JSON.parse(res.body);
      assert.equal(result.args.hello, 'world');
      assert.equal(result.args.query, 'string');
    });

    it('send a get request with headers.', function () {
      var res = wait(req
        .set({'hello': 'world', 'beyond': 'framework', 'number': 1234})
        .end());
      var result = JSON.parse(res.body);
      assert.equal(result.headers.hello, 'world');
      assert.equal(result.headers.beyond, 'framework');
      assert.equal(result.headers.number, '1234');
    });
  });

  describe('#post()', function () {
    var req;
    beforeEach(function () {
      req = request.post('http://localhost:1234/post');
    });

    it('send a post request.', function () {
      var res = wait(req.end());
      var result = JSON.parse(res.body);
      assert.equal(result.url, '/post');
    });

    it('send a post request with form data.', function () {
      var res = wait(req
        .send({'hello': 'world', 'beyond': 'framework'})
        .end());
      var result = JSON.parse(res.body);
      assert.equal(result.form.hello, 'world');
      assert.equal(result.form.beyond, 'framework');
    });

    it('send a post request with JSON data.', function () {
      var res = wait(req
        .json({'hello': 'world', 'beyond': 'framework'})
        .end());
      var result = JSON.parse(res.body);
      var jsonData = JSON.parse(result.data);
      assert.equal(jsonData.hello, 'world');
      assert.equal(jsonData.beyond, 'framework');
    });

    it('send a post request with headers.', function () {
      var res = wait(req
        .set({'hello': 'world', 'beyond': 'framework', 'number': 1234})
        .end());
      var result = JSON.parse(res.body);
      assert.equal(result.headers.hello, 'world');
      assert.equal(result.headers.beyond, 'framework');
      assert.equal(result.headers.number, '1234');
    });
  });

  describe('#put()', function () {
    var req;
    beforeEach(function () {
      req = request.put('http://localhost:1234/put');
    });

    it('send a put request.', function () {
      var res = wait(req.end());
      var result = JSON.parse(res.body);
      assert.equal(result.url, '/put');
    });

    it('send a put request with form data.', function () {
      var res = wait(req
        .send({'hello': 'world', 'beyond': 'framework'})
        .end());
      var result = JSON.parse(res.body);
      assert.equal(result.form.hello, 'world');
      assert.equal(result.form.beyond, 'framework');
    });

    it('send a put request with JSON data.', function () {
      var res = wait(req
        .json({'hello': 'world', 'beyond': 'framework'})
        .end());
      var result = JSON.parse(res.body);
      var jsonData = JSON.parse(result.data);
      assert.equal(jsonData.hello, 'world');
      assert.equal(jsonData.beyond, 'framework');
    });

    it('send a put request with headers.', function () {
      var res = wait(req
        .set({'hello': 'world', 'beyond': 'framework', 'number': 1234})
        .end());
      var result = JSON.parse(res.body);
      assert.equal(result.headers.hello, 'world');
      assert.equal(result.headers.beyond, 'framework');
      assert.equal(result.headers.number, '1234');
    });
  });

  describe('#auth()', function () {
    it('send a request with basic auth.', function () {
      var res = wait(request
        .get('http://localhost:1234/basic-auth')
        .query({'id': 'hello', 'passwd': 'world'})
        .auth('hello', 'world')
        .end());
      var result = JSON.parse(res.body);
      assert.equal(result.authenticated, true);
    });
  });
});
