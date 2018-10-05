var base = base || {};
base.userProfileController = (function() {
    var model = {
        user: {}
    };
    var view = {
        render: function() {
            base.rest.getUser().then(function(u){
                model.user = u;
                return u;
            }).then(function() {
                document.getElementById('set-username').value = model.user.username;
            });  
        },
        clearChanges: function() {
            view.render();
            document.getElementById('set-password').value = '';
            document.getElementById('set-password-confirm').value = '';
        }
    };
    var controller = {
        deleteUser: function() {
            base.rest.deleteUser(model.user.userId)
                .then(base.rest.logout())
                .then(window.location.replace('/'));
        },
        submitUser: function(submitEvent) {
            submitEvent.preventDefault;
            var username = document.getElementById('set-username').value;
            var password = document.getElementById('set-password').value;
            var repeatPassword = document.getElementById('set-password-confirm').value;
            var id = model.user.userId;
            var role = model.user.role.name;
            credentials = {username, password, role};
            if (password === '') {
                delete credentials.password;
            }
            if (password === repeatPassword) {
                base.rest.putUser(id, credentials).then(function(user) {
                    if (user.error) {
                        alert(user.message);
                    } else {
                        view.render();
                        document.getElementById('username').textContent = username;
                    }
                });
            } else {
                alert('Passwords don\'t match');
            }
            return false;
        },
        load: function() {
            document.getElementById('user-form').onsubmit = controller.submitUser;
            document.querySelector('#clear-changes').onclick = view.clearChanges;
            document.querySelector('#delete-account').onclick = controller.deleteUser;
            view.render();
        },
    };

    return controller;
});