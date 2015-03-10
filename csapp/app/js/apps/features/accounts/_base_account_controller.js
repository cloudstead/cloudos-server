App.BaseAccountController = Ember.ObjectController.extend({
	transitionToAccounts: function(){
		this.transitionToRoute('accounts');
	}
});
