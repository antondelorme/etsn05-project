window.base = window.base || {};

window.base.userProfileController = (() => {
    const model = {
        user: {},
        userId: '',
    };

    const view = {
        pad(number) {
            if (number < 10) {
                return '0' + number;
            }
            return number;
        },

        render: () => window.base.rest.getUser(model.userId).then(u => {
            model.user = u;
            return u;
        }).then(() => {
            document.getElementById('set-firstname').value = model.user.firstName;
            document.getElementById('set-lastname').value = model.user.lastName;
            document.getElementById('set-phone-number').value = model.user.phoneNumber;

            if (model.user.gender === 1) {
                document.getElementById('set-female').checked = true;
            } else if (model.user.gender === 0) {
                document.getElementById('set-male').checked = true;
            } else if(model.user.gender === 2) {
                document.getElementById('set-other').checked = true;
            }

            if (model.user.drivingLicense === true) {
                document.getElementById('set-license-true').checked = true;
            } else if (model.user.drivingLicense === false) {
                document.getElementById('set-license-false').checked = true;
            }
        }),

        clearChanges: () => {
            view.render();
            document.getElementById('set-password').value = '';
            document.getElementById('set-password-confirm').value = '';
        }
    };

    const controller = {
        deleteUser: () => {
            element = document.getElementById('user-profile-alert-box');
            element.innerHTML = `<div class="alert alert-danger" role="alert">\n
                                    <h5 class="alert-heading">WARNING</h5>\n
                                    <p>You are trying to delete your account. Once completed this action may not be reversed!</p>\n
                                    <button id="delete-account-confirm" type="button" class="w-100 btn btn-danger">Delete my account anyway</button>                
                                </div>`
            document.getElementById('delete-account-confirm').onclick = () => {
                window.base.rest.deleteUser(model.user.userId)
                    .then(window.base.rest.logout())
                    .then(() => window.location.replace('/'));
            };
        },

        submitUser: submitEvent => {
            submitEvent.preventDefault();

            let gender;
            let drivingLicense;
            const firstName = document.getElementById('set-firstname').value;
            const lastName = document.getElementById('set-lastname').value;
            const phoneNumber = document.getElementById('set-phone-number').value;

            if (document.getElementById('set-male').checked) {
                gender = 0;
            } else if (document.getElementById('set-female').checked) {
                gender = 1;
            } else if(document.getElementById('set-other').checked) {
                gender = 2;
            }

            if (document.getElementById('set-license-true').checked) {
                drivingLicense = true;
            } else if (document.getElementById('set-license-false').checked) {
                drivingLicense = false;
            }

            model.user.firstName = firstName;
            model.user.lastName = lastName;
            model.user.gender = gender;
            model.user.drivingLicense = drivingLicense;
            model.user.phoneNumber = phoneNumber;

            const password = document.getElementById('set-password').value;
            const repeatPassword = document.getElementById('set-password-confirm').value;
            const email = model.user.email;
            const id = model.user.userId;
            const role = model.user.role.name;
            const roleObj = model.user.role;
            model.user.role = role;
            const user = model.user;

            const credentials = {email, password, role, user};

            if (password === '') {
                delete credentials.password;
            }

            if (password === repeatPassword) {
                window.base.rest.putUser(id, {email, password, role, user}).then(user => {
                    if (user.error) {
                        view.render();
                        // TODO: implement alert box
                        alert(user.message);
                    } else {
                        if (!model.loadingWithId) {
                            document.getElementById('navbar-first-name').textContent = firstName;
                        }
                        view.render();
                    }
                });
            } else {
                // TODO: implement alert box
                alert('Passwords don\'t match');
            }
            model.user.role = roleObj;
        },

        load: () => {
            document.getElementById('user-form').onsubmit = controller.submitUser;
            document.querySelector('#clear-changes').onclick = view.clearChanges;
            document.querySelector('#delete-account').onclick = controller.deleteUser;
            view.render();
        },

        loadWithUserId: (id) => {
            model.userId = id;
            model.loadingWithId = true;

            // Change the hash without firing hashchange
            history.pushState({}, '', '#/user-profile');

            controller.load();
        },
    };

    return controller;
});
