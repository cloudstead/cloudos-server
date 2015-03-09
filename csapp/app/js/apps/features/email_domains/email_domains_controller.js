App.EmailDomainsController = Ember.ArrayController.extend({

	mxRecord: function() {
		var mxrecord = this.get("configuration").find(function(x) {
			return x.path === "mxrecord";
		});
		return Ember.isNone(mxrecord) ? "" : mxrecord.value;
	}.property("configuration"),

	actions: {
		doAddDomain: function () {
			var name = this.get('domain');
			if (!Api.add_email_domain(name)) {
				alert('error adding domain: ' + name);
			} else {
				this.send("refreshContent", this);
			}
		},
		doRemoveDomain: function (name) {
			if (confirm('You are about to remove domain "' + name + '". Are you sure?')){
				if (!Api.remove_email_domain(name)) {
					alert('error removing domain: ' + name);
				} else {
					this.send("refreshContent", this);
				}
			}
		}
	}
});
