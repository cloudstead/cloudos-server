App.GroupMembers = Ember.Object.extend({});

App.GroupMembers.reopenClass({
	toArrayFromString: function(string){
		if(string === undefined || string.length === 0){
			return [];
		}
		var recipient_names = string.split(",");
		return recipient_names.map(function(name){
			return name.trim();
		});
	},

	toStringFromArray: function(memberArray){
		if(memberArray === undefined || memberArray.length === 0){
			return "";
		}
		return memberArray.map(function(member) {
			return member.name;
		}).join(", ");
	},
});
