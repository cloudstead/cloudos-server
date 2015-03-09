
App.AccountsController = Ember.ArrayController.extend({
	actions:{
		sortBy: function(property){
			this.set('sortProperties',[property]);
			this.set('sortAscending', !this.get('sortAscending'));
		},

		doBulkDelete: function() {
			var selectedAccounts = App.AccountFilter.filterSelected(this.get('arrangedContent'));

			if (confirm("You are about to delete all selected accounts. Are you sure?")){
				selectedAccounts.forEach(function(account){
					account.destroy();
				});
			}
		},

		doBulkToggleStatus: function(){
			var selectedAccounts = App.AccountFilter.filterSelected(this.get('arrangedContent'));
			App.Account.bulkToggleStatus(selectedAccounts);
		},

		doDeleteAccount: function(account){
			var fullName = account.firstName + " " + account.lastName;
			if (confirm("You are about to delete " + fullName + ". Are you sure?")){
				account.destroy();
			}
		}
	},

	anySelected: function(){
		return App.AccountFilter.anySelected(this.get('arrangedContent'));
	}.property("arrangedContent.@each.isSelected")
});
