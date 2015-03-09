App.ManageAccountAdminChangePasswordRoute = Ember.Route.extend({
	model: function () {
		return this.modelFor('manageAccount');
	},
	renderTemplate: function() {
		this.render('manageAccount/adminChangePassword', { outlet: 'change_password', controller: this.controller });
	}
});
