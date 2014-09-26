var crypto = require('crypto');

exports.test = function () {
    var result = {};

    // signToken
    result.signToken = crypto.signToken('token-string');

    // sign
    result.sign1 = crypto.sign('Hello, Sign1!');
    result.sign2 = crypto.sign('Hello, Sign2!', 'key-string');

    // generateToken
    result.generateToken = crypto.generateToken();

    // generateSignedToken
    result.generateSignedToken = crypto.generateSignedToken();

    // extractSignedToken
    result.extractSignedToken = crypto.extractSignedToken(result.signToken);

    // encryptAES
    result.encryptAES1 = crypto.encryptAES('Hello, Encryption1!');
    result.encryptAES2 = crypto.encryptAES('Hello, Encryption2!', 'some-private-key');

    // decryptAES
    result.decryptAES1 = crypto.decryptAES(result.encryptAES1);
    result.decryptAES2 = crypto.decryptAES(result.encryptAES2, 'some-private-key');


    // constantTimeEquals
    result.constantTimeEquals = crypto.constantTimeEquals('Hello, String1', 'Hello, String2');

    // compareSignedTokens
    result.compareSignedTokens = crypto.compareSignedTokens(result.signToken, crypto.signToken('token-string2'));

    return result;
};
