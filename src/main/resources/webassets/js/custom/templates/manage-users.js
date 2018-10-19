window.base = window.base || {};

window.base.manageUsersController = (() => {
    const model = {
        users: [], // All users
        searchedUsers: [] // The users selected by search.
    };

    const view = {
        renderCard: user => {
            const t = document.getElementById('manage-users-template');

            t.content.querySelector('.admin-name').textContent = user.firstName + ' ' + user.lastName;
            t.content.querySelector('.admin-email').textContent = user.email;

            if (user.numberOfRatings === 0) {
                t.content.querySelector('.admin-rating').textContent = 'No rating yet';
            } else {
                t.content.querySelector('.admin-rating').textContent = user.ratingTotalScore / user.numberOfRatings + 'rating';
            }

            if (user.warning == null) {
                user.warning = 0;
            }

            t.content.querySelector('.admin-warning').textContent = user.warning + ' warnings';
            t.content.querySelector('.admin-warning').id = 'admin-warning' + user.userId;

            // Associate buttons and cards with correct Id
            t.content.querySelector('.edit-profile-button').id = 'edit-profile' + user.userId;
            t.content.querySelector('.give-warning-button').id = 'give-warning' + user.userId;
            t.content.querySelector('.delete-account-button').id = 'delete-account' + user.userId;
            t.content.querySelector('.manage-users-user').id = 'manage-user-card' + user.userId;

            const clone = document.importNode(t.content, true);
            t.parentElement.appendChild(clone);

            // Add functionality to buttons.
            document.getElementById('edit-profile' + user.userId).onclick = () => controller.editProfile(user);
            document.getElementById('give-warning' + user.userId).onclick = () => controller.giveWarning(user);
            document.getElementById('delete-account' + user.userId).onclick = () => controller.deleteAccount(user);
        },

        clearRender: () => {
            model.users.forEach(user => {
                const item = document.getElementById('manage-user-card' + user.userId);

                if (item !== null) {
                    item.parentElement.removeChild(item);
                }
            });
        },

        render: function () {
            view.clearRender();
            model.searchedUsers.forEach(user => view.renderCard(user));
        },
    };

    const controller = {
        giveWarning: function (user) {
            window.base.rest.warnUser(user.userId).then(() => {
                user.warning = user.warning + 1;
                document.getElementById('admin-warning' + user.userId).textContent = user.warning + ' warnings';
            });
        },

        remove: (array, element) => array.filter(e => e !== element),

        deleteAccount: user => {
            element = document.getElementById('delete-account' + user.userId);
            element.innerHTML = '<div class="alert alert-danger" role="alert">\n' + 
                                    '<h5 class="alert-heading">WARNING</h5>\n'+
                                    '<button id="delete-account-confirm' + user.userId + '" type="button" class="w-100 btn btn-danger">Delete</button>\n' +               
                                '</div>';
            document.getElementById('delete-account-confirm'+user.userId).onclick = () => {
                window.base.rest.deleteUser(user.userId).then(function () {
                    // Remove user from lists so next search is correct.
                    model.users = controller.remove(model.users, user);
                    model.searchedUsers = controller.remove(model.searchedUsers, user);
    
                    // Remove element (This can't be done in render since it won't find 
                    // the user due to it being removed in the list.)
                    const item = document.getElementById('manage-user-card' + user.userId);
                    item.parentElement.removeChild(item);
                });
            };
        },

        editProfile: user => {
            fetch('templates/user-profile.html')
                .then(response => response.text())
                .then(tabHtml => {
                    document.getElementById('main-tab').innerHTML = tabHtml;
                    window.base.userProfileController().loadWithUserId(user.userId);
                });
        },

        search: () => {
            const searchString = document.getElementById('search-email').value;
            model.searchedUsers = []; // Reset searched users.

            model.users.forEach(user => {
                if (user.email.toLowerCase().trim().indexOf(searchString.toLowerCase().trim()) >= 0) {
                    model.searchedUsers.push(user);
                }
            });

            view.render();
        },

        reset: () => {
            model.searchedUsers = model.users;
            document.getElementById('search-email').value = '';
            view.render();
        },

        load: () => {
            document.getElementById('search-user-button').onclick = () => controller.search();
            document.getElementById('search-user-rest-button').onclick = () => controller.reset();

            base.rest.getUsers().then(users => {
                model.users = users;
                model.searchedUsers = users;
                return users;
            }).then(() => view.render());
        }
    };

    return controller;
});
