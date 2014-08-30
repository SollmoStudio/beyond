var path = require('path');

exports.test = function () {
    var result = {};

    result['normalize'] = path.normalize('/foo/bar//baz/asdf/quux/..'); // '/foo/bar/baz/asdf'
    result['join'] = path.join('/foo', 'bar', 'baz/asdf', 'quux', '..'); // '/foo/bar/baz/asdf'
    result['resolve1'] = path.resolve('/foo/bar', './baz'); // '/foo/bar/baz'
    result['resolve2'] = path.resolve('/foo/bar', '/tmp/file/'); // '/tmp/file'
    result['resolve3'] = path.resolve('wwwroot', 'static_files/png/', '../gif/image.gif'); // '/abs/path/wwwroot/static_files/gif/image.gif'
    result['relative'] = path.relative('/data/orandea/test/aaa', '/data/orandea/impl/bbb'); // '../../impl/bbb'
    result['dirname'] = path.dirname('/foo/bar/baz/asdf/quux'); // '/foo/bar/baz/asdf'
    result['basename1'] = path.basename('/foo/bar/baz/asdf/quux.html'); // 'quux.html'
    result['basename2'] = path.basename('/foo/bar/baz/asdf/quux.html', '.html'); // 'quux'
    result['extname1'] = path.extname('index.html'); // '.html'
    result['extname2'] = path.extname('index.coffee.md'); // '.md'
    result['extname3'] = path.extname('index.'); // '.'
    result['extname4'] = path.extname('index'); // ''
    result['sep'] = path.sep; // '\\' or '/'
    result['delimiter'] = path.delimiter; // ';' or ':'

    return result;
};

