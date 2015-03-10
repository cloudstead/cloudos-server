Timer = {
	ms_in_s: 1000,
	ms2s: function(ms) {
		return ms/this.ms_in_s;
	},
	s2ms: function(s) {
		return s * this.ms_in_s;
	}
}
