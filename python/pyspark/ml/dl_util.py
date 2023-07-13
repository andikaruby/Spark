import os
import tempfile
import textwrap
from typing import Any, Callable

from pyspark import cloudpickle


class FunctionPickler:
    """
    This class provides a way to pickle a function and its arguments.
    It also provides a way to create a script that can run a
    function with arguments if they have them pickled to a file.
    It also provides a way of extracting the conents of a pickle file.
    """

    @staticmethod
    def pickle_fn_and_save(
        fn: Callable, file_path: str, save_dir: str, *args: Any, **kwargs: Any
    ) -> str:
        """
        Given a function and args, this function will pickle them to a file.

        Parameters
        ----------
        fn: Callable
            The picklable function that will be pickled to a file.

        file_path: str
            The path where to save the pickled function, args, and kwargs. If it's the
            empty string, the function will decide on a random name.

        save_dir: str
            The directory in which to save the file with the pickled function and arguments.
            Does nothing if the path is specified. If both file_path and save_dir are empty,
            the function will write the file to the current working directory with a random
            name.

        *args: Any
            Arguments of fn that will be pickled.

        **kwargs: Any
            Key word arguments to fn that will be pickled.

        Returns
        -------
        str:
            The path to the file where the function and arguments are pickled.
        """
        if file_path != "":
            with open(file_path, "wb") as f:
                cloudpickle.dump((fn, args, kwargs), f)
                return f.name

        if save_dir == "":
            save_dir = os.getcwd()

        with tempfile.NamedTemporaryFile(dir=save_dir, delete=False) as f:
            cloudpickle.dump((fn, args, kwargs), f)
            return f.name

    @staticmethod
    def create_fn_run_script(
        pickled_fn_path: str,
        fn_output_path: str,
        script_path: str,
        prefix_code: str = "",
        suffix_code: str = "",
    ) -> str:
        """
        Given a file containing a pickled function and arguments, this function will create a
        pytorch file that will execute the function and pickle the functions outputs.

        Parameters
        ----------
        pickled_fn_path:
            This is the path of the file containing the pickled function, args, and kwargs.

        fn_output_path: str
            This is the location where the created file will save the pickled output of
            the function.

        script_path: str
            This is the path which will be used for the created pytorch file.

        prefix_code: str
            This contains a string that the user can pass in which will be executed before
            the code generated by this class to execute the function and save it. If
            prefix_code is the empty string, nothing will be written before the auto-
            generated code.

        suffix_code: str
            This contains a string of code that the user can pass in which will be executed
            after the code generated by this class finishes executing. If suffix_code is
            the empty string, nothing will be written after the auto-generated code.

        Returns
        -------
        str:
            The path to the location of the newly created pytorch file.
        """

        code_snippet = textwrap.dedent(
            f"""
                    from pyspark import cloudpickle
                    import os

                    if __name__ == "__main__":
                        with open("{pickled_fn_path}", "rb") as f:
                            fn, args, kwargs = cloudpickle.load(f)
                        output = fn(*args, **kwargs)
                        with open("{fn_output_path}", "wb") as f:
                            cloudpickle.dump(output, f)
                    """
        )
        with open(script_path, "w") as f:
            if prefix_code != "":
                f.write(prefix_code)
            f.write(code_snippet)
            if suffix_code != "":
                f.write(suffix_code)

        return script_path

    @staticmethod
    def get_fn_output(fn_output_path: str) -> Any:
        """
        Given a path to a file with pickled output, this function
        will unpickle the output and return it to the user.

        Parameters
        ----------
        fn_output_path: str
            The path to the file containing the pickled output of a function.

        Returns
        -------
        Any:
            The unpickled output stored in func_output_path
        """
        with open(fn_output_path, "rb") as f:
            return cloudpickle.load(f)
