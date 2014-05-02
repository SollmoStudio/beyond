/** @jsx React.DOM */
define(["react"], function(React) {
    return React.createClass({
        handleSubmit: function () {
            var username = this.refs.username.getDOMNode().value.trim();
            var password = this.refs.password.getDOMNode().value.trim();
            // FIXME: Add client-side validation code here.
            return true;
        },
        // FIXME: autofocus does not work.
        // FIXME: Remember me button should work.
        render: function () {
            return (
                <form className="form-signin" role="form" action={this.props.postUrl} method="POST" onSubmit={this.handleSubmit}>
                    <h2 className="form-signin-heading">{this.props.heading}</h2>
                    <input type="text" className="form-control" name="username" placeholder="username" ref="username" required autofocus />
                    <input type="password" className="form-control" name="password" placeholder="password" ref="password" required />
                    <label className="checkbox">
                        <input type="checkbox" value="remember-me" />
                    Remember me
                    </label>
                            {this.props.errorMessage ? <div className="alert alert-danger">{this.props.errorMessage}</div> : ''}
                    <button className="btn btn-lg btn-primary btn-block" type="submit">Sign in</button>
                </form>
                );
        }
    });
});

