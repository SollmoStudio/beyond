/* global console */
var Buffer = require('buffer').Buffer;
var http = require('http');
var qsparser = require('querystring').parse;
var urlparser = require('url').parse;

function notFound(res) {
  res.writeHead(404);
  res.end('Not found.');
}

var handlers = {};
var server = http.createServer(function (req, res) {
  var url = urlparser(req.url, true);

  if (handlers[url.pathname]) {
    handlers[url.pathname](url, req, res);
  } else {
    notFound(res);
  }
});

handlers['/get'] = function (url, req, res) {
  if (req.method === 'GET') {
    res.end(JSON.stringify({
      url: req.url,
      headers: req.headers,
      args: url.query
    }));
  } else {
    notFound(res);
  }
};

handlers['/post'] = handlers['/put'] = function (url, req, res) {
  if (req.method === 'POST' || req.method === 'PUT') {
    var data = '';

    req.on('data', function (chunk) {
      data += chunk.toString();
    });

    req.on('end', function () {
      var form = null;

      var contentType = req.headers['content-type'];
      if (typeof contentType === 'string' &&
          contentType.indexOf('application/x-www-form-urlencoded') >= 0) {
        form = qsparser(data);
      }

      res.end(JSON.stringify({
        url: req.url,
        headers: req.headers,
        form: form,
        data: data
      }));
    });
  } else {
    notFound(res);
  }
};

function authenticate(req) {
  var auth = req.headers.authorization;
  if (!auth) {
    return null;
  }

  // malformed
  var parts = auth.split(' ');
  if (parts[0].toLowerCase() !== 'basic' || !parts[1]) {
    return null;
  }
  auth = parts[1];

  // credentials
  auth = new Buffer(auth, 'base64').toString();
  auth = auth.match(/^([^:]*):(.*)$/);
  if (!auth) {
    return null;
  }

  return { id: auth[1], passwd: auth[2] };
}

handlers['/basic-auth'] = function (url, req, res) {
  if (req.method === 'GET') {
    var id = url.query.id;
    var passwd = url.query.passwd;

    var auth = authenticate(req);

    res.end(JSON.stringify({
      authenticated: (!!auth && auth.id === id && auth.passwd === passwd)
    }));
  } else {
    notFound(res);
  }
};

handlers['/quit'] = function (url, req, res) {
  res.end();
  server.close();
  throw new Error('quit');
};

server.listen(1234, function () {
  console.log('listening');
});
