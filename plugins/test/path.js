var assert = require('assert');
var path = require('path');
require('bdd').mount(this);

describe('path', function () {
  describe('#normalize()', function () {
    it('should normalize duplicated slashes and dots', function () {
      assert.equal(path.normalize('/foo/bar//baz/asdf/quux/..'), '/foo/bar/baz/asdf');
    });
  });

  describe('#join()', function () {
    it('should join paths and normalize the result', function () {
      assert.equal(path.join('/foo', 'bar', 'baz/asdf', 'quux', '..'), '/foo/bar/baz/asdf');
    });
  });

  describe('#resolve()', function () {
    it('should resolve a relative path', function () {
      assert.equal(path.resolve('/foo/bar', './baz'), '/foo/bar/baz');
    });

    it('should re-resolve from a path if it\'s absolute', function () {
      assert.equal(path.resolve('/foo/bar', '/tmp/file/'), '/tmp/file');
    });

    it('should find an absolute path if it starts from a relative path', function () {
      // get the current working directory from Java
      var currentDir = java.lang.System.getProperty('user.dir');
      assert.equal(path.resolve('wwwroot', 'static_files/png/', '../gif/image.gif'),
                   currentDir + '/wwwroot/static_files/gif/image.gif');
    });
  });

  describe('#relative()', function () {
    it('should get the relative path betweeh one another', function () {
      assert.equal(path.relative('/data/orandea/test/aaa', '/data/orandea/impl/bbb'),
                   '../../impl/bbb');
    });
  });

  describe('#dirname()', function () {
    it('should get the directory name of the provided path', function () {
      assert.equal(path.dirname('/foo/bar/baz/asdf/quux'), '/foo/bar/baz/asdf');
    });
  });

  describe('#basename()', function () {
    it('should return the base name of the provided path', function () {
      assert.equal(path.basename('/foo/bar/baz/asdf/quux.html'), 'quux.html');
    });

    it('should return the base name without the extension if provided', function () {
      assert.equal(path.basename('/foo/bar/baz/asdf/quux.html', '.html'), 'quux');
    });
  });

  describe('#extname()', function () {
    it('should return the extension of the provided path', function () {
      assert.equal(path.extname('index.html'), '.html');
    });

    it('should return the last extension if there are two dots in the path', function () {
      assert.equal(path.extname('index.coffee.md'), '.md');
    });

    it('should return dot if the path ends with dot', function () {
      assert.equal(path.extname('index.'), '.');
    });

    it('should return empty string if there\'s no dot in the path', function () {
      assert.equal(path.extname('index'), '');
    });
  });

  function isWindows () {
    return java.lang.System.getProperty('os.name').search('Windows') > 0;
  }

  describe('#sep', function () {
    it('should be OS specific separators for each OS', function () {
      assert.equal(path.sep, isWindows() ? '\\' : '/');
    });
  });

  describe('#delimiter', function () {
    it('should be OS specific delimiters for each OS', function () {
      assert.equal(path.delimiter, isWindows() ? ';' : ':');
    });
  });
});
