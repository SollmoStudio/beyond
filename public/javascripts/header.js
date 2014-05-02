/** @jsx React.DOM */
define(["react"], function(React) {
    return React.createClass({
        render: function() {
            var liElements = $.map(this.props.menus, function (value, key) {
                return <li>
                    <a href={value}>{key}</a>
                </li>;
            });

            return (
                <div className="navbar navbar-inverse navbar-fixed-top" role="navigation">
                    <div className="container-fluid">
                        <div className="navbar-header">
                            <a className="navbar-brand" href="#">{this.props.projectName}</a>
                        </div>
                        <div className="navbar-collapse collapse">
                            <ul className="nav navbar-nav navbar-right">
                                    {liElements}
                            </ul>
                        </div>
                    </div>
                </div>
                );
        }
    });
});
