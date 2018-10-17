window.base = window.base || {};

window.base.changeLocation = url => window.location.replace(url);

window.base.registerController = (() => {
    const controller = {
        setInputListeners: form => {
            const dob = form.dateOfBirth.getField();
            dob.addEventListener('focusin', () => {
                dob.setAttribute('placeholder', 'YYYY-MM-DD');
            });

            dob.addEventListener('focusout', () => {
                dob.setAttribute('placeholder', 'Date of birth');
            });

            for (let e in form) {
                if (form.hasOwnProperty(e) &&
                    typeof form[e].getField !== 'undefined' &&
                    typeof form[e].validate !== 'undefined') {

                    if (e === 'gender') {
                        controller.validateGenderInput(e, form);
                    } else if (e !== 'drivingLicense') {
                        controller.validateTextInput(e, form);
                    }
                }
            }
        },
        sanitizeDateInput: field => {
            let input = field.value;
            input = input.replace(/[^\d]+/g, '');
            input = input.length > 8 ? input.slice(0, 8) : input;

            if (input.length > 4 && input.indexOf('-') !== 4) {
                input = input.slice(0, 4) + '-' + input.slice(4);
            }

            if (input.length > 7 && input.lastIndexOf('-') !== 6) {
                input = input.slice(0, 7) + '-' + input.slice(7);
            }

            field.value = input;
        },
        validateGenderInput: (e, form) => {
            form[e].getField().onchange = () => {
                if (form[e].validate()) {
                    form[e].isValid = true;
                } else {
                    form[e].isValid = false;
                }
                controller.validateForm(form);
            };

            form[e].getFieldFemale().onchange = () => {
                if (form[e].validate()) {
                    form[e].isValid = true;
                } else {
                    form[e].isValid = false;
                }

                controller.validateForm(form);
            }
        },
        validateTextInput: (e, form) => {
            const field = form[e].getField();
            field.onkeyup = () => {
                if (e === 'dateOfBirth') {
                    controller.sanitizeDateInput(form[e].getField());
                }
                const inputClassList = field.classList;
                const spanClassList = field.nextElementSibling.children[0].classList;
                const iconClassList = field.nextElementSibling.children[0].children[0].classList;

                iconClassList.remove('text-muted');

                if (form[e].validate()) {
                    form[e].isValid = true;

                    if (inputClassList.contains('border-danger')) {
                        spanClassList.remove('border-danger');
                        iconClassList.remove('text-danger');
                        inputClassList.remove('border-danger');
                    }

                    if (!inputClassList.contains('border-success')) {
                        spanClassList.add('border-success');
                        iconClassList.add('text-success');
                        inputClassList.add('border-success');
                    }
                } else {
                    form[e].isValid = false;

                    if (inputClassList.contains('border-success')) {
                        spanClassList.remove('border-success');
                        iconClassList.remove('text-success');
                        inputClassList.remove('border-success');
                    }

                    if (!inputClassList.contains('border-danger')) {
                        spanClassList.add('border-danger');
                        iconClassList.add('text-danger');
                        inputClassList.add('border-danger');
                    }
                }

                controller.validateForm(form);
            }
        },
        validateForm: form => {
            for (let e in form) {
                if (form.hasOwnProperty(e) &&
                    typeof form[e].isValid !== 'undefined' &&
                    !form[e].isValid) {
                    if (!form.submit.hasAttribute('disabled')) {
                        form.submit.setAttribute('disabled', '');
                    }
                    return;
                }
            }

            if (form.submit.hasAttribute('disabled')) {
                form.submit.removeAttribute('disabled');
            }
        },
        getForm: function () {
            return {
                submit: document.getElementById('register'),
                form: document.getElementById('register-form'),
                email: {
                    getField: () => document.getElementById('register-email'),
                    getValue: () => document.getElementById('register-email').value,
                    validate: () => document.getElementById('register-email').checkValidity(),
                    isValid: false
                },
                password: {
                    getField: () => document.getElementById('register-password'),
                    getValue: () => document.getElementById('register-password').value,
                    validate: () => document.getElementById('register-password').checkValidity(),
                    isValid: false
                },
                firstName: {
                    getField: () => document.getElementById('register-first-name'),
                    getValue: () => document.getElementById('register-first-name').value,
                    validate: () => document.getElementById('register-first-name').checkValidity(),
                    isValid: false
                },
                lastName: {
                    getField: () => document.getElementById('register-last-name'),
                    getValue: () => document.getElementById('register-last-name').value,
                    validate: () => document.getElementById('register-last-name').checkValidity(),
                    isValid: false
                },
                phoneNumber: {
                    getField: () => document.getElementById('register-phone-number'),
                    getValue: () => document.getElementById('register-phone-number').value,
                    validate: () => document.getElementById('register-phone-number').checkValidity(),
                    isValid: false
                },
                dateOfBirth: {
                    getField: () => document.getElementById('register-date-of-birth'),
                    getValue: () => Date.parse(document.getElementById('register-date-of-birth').value),
                    validate: () => {
                        const field = document.getElementById('register-date-of-birth');
                        const LEGAL_AGE = 1000 * 60 * 60 * 24 * 364 * 18;
                        return field.checkValidity() && Date.now() - Date.parse(field.value) >= LEGAL_AGE;
                    },
                    isValid: false
                },
                drivingLicence: {
                    getField: () => document.getElementById('register-driving-license'),
                    getValue: () => document.getElementById('register-driving-license').checked,
                    validate: () => document.getElementById('register-driving-license').checkValidity(),
                },
                gender: {
                    getField: () => document.getElementById('register-gender-male'),
                    getFieldFemale: () => document.getElementById('register-gender-female'),
                    getValue: () => document.getElementById('register-gender-male').checked ? 0 : 1,
                    validate: () => document.getElementById('register-gender-male').checked ||
                        document.getElementById('register-gender-female').checked,
                    isValid: false
                }
            }
        },
        submitUser: submitEvent => {
            submitEvent.preventDefault();
            let {email, password, firstName, lastName, phoneNumber, dateOfBirth, drivingLicence, gender} = controller.getForm();

            email = email.getValue();
            password = password.getValue();
            firstName = firstName.getValue();
            lastName = lastName.getValue();
            phoneNumber = phoneNumber.getValue();
            dateOfBirth = dateOfBirth.getValue();
            drivingLicence = drivingLicence.getValue();
            gender = gender.getValue();

            const user = {email, firstName, lastName, phoneNumber, dateOfBirth, drivingLicence, gender};

            const role = "USER";
            const credentials = {email, password, role, user};

            window.base.rest.addUser(credentials).then(user => {
                if (user.error) {
                    controller.showFailure(user.message);
                } else {
                    window.base.rest.login(email, password, false).then(response => {
                        if (response.ok) {
                            window.base.changeLocation('/');
                        } else {
                            controller.showFailure(user.message);
                        }
                    });
                }
            });

            return false;
        },
        showFailure: msg => {
            const alert = document.getElementById('register-alert');
            const classList = alert.classList;

            if (classList.contains('d-none')) {
                classList.remove('d-none');
            }

            alert.innerHTML = '<h5 class="alert-heading">Oops!</h5>'
                + '<p>Something went wrong, error message: ' + msg + '</p>';
        },
        load: () => {
            let form = controller.getForm();
            controller.setInputListeners(form);
            form.form.onsubmit = controller.submitUser;
        },
        initOnLoad: () => document.addEventListener('DOMContentLoaded', window.base.registerController.load)
    };

    return controller;
})
();
