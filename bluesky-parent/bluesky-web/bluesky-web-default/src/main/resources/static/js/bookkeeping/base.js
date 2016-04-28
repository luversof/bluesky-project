_.extend( Backbone.Collection.prototype, {
    
   // Comma separated list of attributes
   sortColumn: null, 
  
   // Comma separated list corresponding to column list
   sortDirection: 'asc', // - [ 'asc'|'desc' ]
 
   comparator: function( a, b ) {
 
      if ( !this.sortColumn ) return 0;
 
      var cols = this.sortColumn.split( ',' ),
          dirs = this.sortDirection.split( ',' ),
          cmp;
 
      // First column that does not have equal values
      cmp = _.find( cols, function( c ) { return a.attributes[c] != b.attributes[c]; });
 
      // undefined means they're all equal, so we're done.
      if ( !cmp ) return 0;
 
      // Otherwise, use that column to determine the order
      // match the column sequence to the methods for ascending/descending
      // default to ascending when not defined.
      if ( ( dirs[_.indexOf( cols, cmp )] || 'asc' ).toLowerCase() == 'asc' ) {
         return a.attributes[cmp] > b.attributes[cmp] ? 1 : -1;
      } else {
         return a.attributes[cmp] < b.attributes[cmp] ? 1 : -1;
      }
 
   },
 
});