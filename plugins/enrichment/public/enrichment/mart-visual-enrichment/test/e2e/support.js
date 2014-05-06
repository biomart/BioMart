

protractor.promise.all = function(arr) {
  var n = arr.length;
  if (!n) {
    return protractor.promise.fulfilled([]);
  }

  var toFulfill = n;
  var result = protractor.promise.defer();
  var values = [];

  var onFulfill = function(index, value) {
    values[index] = value;
    toFulfill--;
    if (toFulfill == 0) {
      result.fulfill(values);
    }
  };

  function partial (fn) {
    var args = [].slice.call(arguments, 1);
    return function () {
      fn.apply(null, args.concat(arguments));
    };
  }

  for (var i = 0; i < n; ++i) {
    protractor.promise.asap(
        arr[i], partial(onFulfill, i), result.reject);
  }

  return result.promise;
};
