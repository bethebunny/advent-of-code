mod aoc_client;

use std::boxed::Box;
use std::collections::HashMap;
use std::iter;
use std::ptr::NonNull;

#[derive(Debug)]
enum File {
    Binary {
        bytes: usize,
    },
    Directory {
        files: HashMap<String, File>,
        parent: NonNull<File>,
    },
}

#[derive(Debug)]
enum Error {
    FileKindError,
    NoSuchFile,
    ParseError,
}

impl File {
    fn new_root() -> File {
        let mut root = File::Directory {
            files: HashMap::new(),
            parent: NonNull::dangling(),
        };
        let root_ptr = NonNull::from(&mut root);
        if let File::Directory { ref mut parent, .. } = root {
            *parent = root_ptr
        }
        root
    }

    fn new_dir(parent: &File) -> File {
        File::Directory {
            files: HashMap::new(),
            parent: NonNull::from(parent),
        }
    }

    fn size(&self) -> usize {
        match self {
            File::Binary { bytes } => *bytes,
            File::Directory { files, .. } => files.values().map(|f| f.size()).sum(),
        }
    }

    fn walk_dirs(&self) -> Box<dyn Iterator<Item = &File> + '_> {
        match self {
            File::Binary { .. } => Box::new(iter::empty()),
            File::Directory { files, .. } => {
                Box::new(iter::once(self).chain(files.values().flat_map(|f| f.walk_dirs())))
            }
        }
    }

    fn files_mut(&mut self) -> Result<&mut HashMap<String, File>, Error> {
        match self {
            File::Directory { files, .. } => Ok(files),
            _ => Err(Error::FileKindError),
        }
    }

    #[allow(dead_code)]
    fn files(&self) -> Result<&HashMap<String, File>, Error> {
        match self {
            File::Directory { files, .. } => Ok(files),
            _ => Err(Error::FileKindError),
        }
    }
}

fn parse_filesystem_from_logs(logs: &str) -> Result<File, Error> {
    let mut root = File::new_root();
    let mut cwd = &mut root;

    for line in logs.lines() {
        if line == "$ cd /" {
            cwd = &mut root;
        } else if line == "$ cd .." {
            if let File::Directory { parent, .. } = cwd {
                // Yup this is unsafe :P
                // I think the fix here is to make our file pointers Rc<RefCell<File>>
                // and then have parent be a Weak<RefCell<File>> so we can enforce
                // run-time borrow semantics safely.
                cwd = unsafe { parent.as_mut() };
            }
        } else if line.starts_with("$ cd ") {
            let (_, subdir) = line.rsplit_once(" ").ok_or(Error::ParseError)?;
            cwd = match cwd.files_mut()?.get_mut(subdir).ok_or(Error::NoSuchFile)? {
                File::Binary { .. } => return Err(Error::FileKindError),
                dir => dir,
            };
        } else if line.starts_with("$ ls") {
            // nothing
        } else if line.starts_with("dir ") {
            let (_, subdir) = line.rsplit_once(" ").ok_or(Error::ParseError)?;
            let new_dir = File::new_dir(cwd);
            cwd.files_mut()?.insert(subdir.to_string(), new_dir);
        } else {
            let (size, name) = line.split_once(" ").ok_or(Error::ParseError)?;
            let bytes = size.parse::<usize>().map_err(|_| Error::ParseError)?;
            cwd.files_mut()?
                .insert(name.to_string(), File::Binary { bytes });
        }
    }

    Ok(root)
}

fn main() {
    let data = aoc_client::input(2022, 7).unwrap();
    let root = parse_filesystem_from_logs(data.as_str()).unwrap();

    let small_dirs = root.walk_dirs().filter(|d| d.size() <= 100000);
    println!("{}", small_dirs.map(|d| d.size()).sum::<usize>());

    let max_space = 70000000;
    let required_free_space = 30000000;
    let used_space = root.size();
    if used_space < max_space - required_free_space {
        println!("Good news! Enough free space already.");
    } else {
        let required_to_free = used_space - (max_space - required_free_space);
        println!(
            "{}",
            root.walk_dirs()
                .map(|d| d.size())
                .filter(|&s| s >= required_to_free)
                .min()
                .unwrap(),
        );
    }
}
