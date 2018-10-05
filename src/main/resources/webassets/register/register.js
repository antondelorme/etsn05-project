var base = base || {};
base.changeLocation = function(url) {
    window.location.replace(url);
};
base.registerController = (function() {
    var controller = {
        submitUser: function(submitEvent) {
            submitEvent.preventDefault;
            var password = document.getElementById('register-password').value;
            var username = document.getElementById('register-username').value;
            var role = "USER";
            var credentials ={username, password, role};
            base.rest.addUser(credentials).then(function(user) {
                if (user.error) {
                    alert(user.message);
                } else {
                    base.rest.login(username, password, false).then(function(response) {
                        if (response.ok) {
                            base.changeLocation('/');
                        } else {
                            alert('Error during login.');
                        }
                    });
                }
            });


            return false;
        },

        load: function() {
            document.getElementById('register-form').onsubmit = controller.submitUser;
        },
        initOnLoad: function() {
            document.addEventListener('DOMContentLoaded', base.registerController.load);
        }
    };
    
    return controller;
})();
