describe('drive specs', function() {
    var none = new base.User({username: '-', role: 'NONE', id: 0});
    var admin = new base.User({username: 'Admin', role: 'ADMIN', id: 1});

    describe('top row', function() {
        it('should have correct text in top row', function() {
            expect(document.getElementById('...').value).toBe('...');
        })
    })
});