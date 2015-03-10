App.AccountFilter = Ember.Object.reopenClass({
	filterSelected: function(accounts) {
		return accounts.filter(function(account){
				return account.get('isSelected');
			});
	},
	anySelected: function(accounts) {
		return App.AccountFilter.filterSelected(accounts).length > 0 ? true : false;
	}
});
