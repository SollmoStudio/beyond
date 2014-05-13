/** @jsx React.DOM */
define(["jquery", "react", "bootstrap"], function ($, React) {
    return React.createClass({
        render: function() {
            var selectedMenu = this.props.selectedMenu;
            var liElements = $.map(this.props.menus, function (value, key) {
                var li = <li>
                     <a href={value}>{key}</a>
                 </li>;
                if (key === selectedMenu) {
                    li = <li className="active">
                        <a href={value}>{key}</a>
                    </li>;
                }
                return li;
            });

            return (
                <div>
                    <h3>{this.props.title}</h3>
                    <ul className="nav nav-sidebar">
                        {liElements}
                    </ul>
                </div>
            );
        }
    });
});
