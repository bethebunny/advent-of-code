# def get_data(day: Int) -> String:
#     local_cache_path = DATA_DIR / (str(day) + ".input")
#     if not local_cache_path.exists():
#         requests = Python.import_module("requests")
#         cookies = Python.dict()
#         cookies["session"] = SESSION_KEY
#         args = PythonObject((data_url(day),))
#         kwargs = Python.dict()
#         kwargs["cookies"] = PythonObject(cookies.py_object)
#         response = call_with_kwargs(requests.get, args, kwargs.py_object)
#         if not response.ok():
#             raise "request failed"
#         python_builtins = Python.import_module("__builtins__")
#         context = python_builtins.open(str(local_cache_path), "w")
#         fp = context.__enter__()
#         try:
#             fp.write(response.text)
#         finally:
#             print("trying to exit, idk")
#             # fp.__exit__()  # TODO(issue) WTF
#             # whatever just leak it

#     with open(local_cache_path, "r") as local_cache_file:
#         return local_cache_file.read()


# fn call_with_kwargs(
#     self: PythonObject, args: PythonObject, kwargs: PythonObject
# ) raises -> PythonObject:
#     """Call the underlying object as if it were a function.

#     Returns:
#         The return value from the called object.
#     """
#     var cpython = _get_global_python_itf().cpython()
#     let callable = self.py_object
#     cpython.Py_IncRef(callable)
#     cpython.Py_IncRef(args.py_object)
#     cpython.Py_IncRef(kwargs.py_object)
#     let result = PyObject_CallObject(
#         cpython, callable, args.py_object, kwargs.py_object
#     )
#     cpython.Py_DecRef(callable)
#     cpython.Py_DecRef(args.py_object)
#     cpython.Py_DecRef(kwargs.py_object)
#     Python.throw_python_exception_if_error_state(cpython)
#     # Python always returns non null on success.
#     # A void function returns the singleton None.
#     # If the result is null, something went awry;
#     # an exception should have been thrown above.
#     if result.is_null():
#         raise Error(
#             "Call returned null value, indicating failure. Void functions"
#             " return NoneType."
#         )
#     return PythonObject(result)


# from python.cpython import CPython, PyObjectPtr


# fn PyObject_CallObject(
#     inout self: CPython,
#     callable: PyObjectPtr,
#     args: PyObjectPtr,
#     kwargs: PyObjectPtr,
# ) -> PyObjectPtr:
#     let r = self.lib.get_function[
#         fn (PyObjectPtr, PyObjectPtr, PyObjectPtr) -> PyObjectPtr
#     ]("PyObject_CallObject")(callable, args, kwargs)
#     self._inc_total_rc()
#     return r
