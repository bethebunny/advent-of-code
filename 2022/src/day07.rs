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

impl File {
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
}

fn main() {
    let data = aoc_client::input(2022, 7).unwrap();

    let mut root = unsafe {
        let mut dummy = File::Binary { bytes: 0 };
        let mut _root = File::Directory {
            files: HashMap::new(),
            parent: NonNull::new_unchecked(&mut dummy as *mut File),
        };
        let root_ptr: *mut File = &mut _root;
        if let File::Directory { ref mut parent, .. } = _root {
            *parent = NonNull::new_unchecked(root_ptr);
        }
        _root
    };
    let mut cwd = &mut root;

    for line in data.lines() {
        if line == "$ cd /" {
            cwd = &mut root;
        } else if line == "$ cd .." {
            if let File::Directory { parent, .. } = cwd {
                cwd = unsafe { parent.as_mut() };
            }
        } else if line.starts_with("$ cd ") {
            let (_, subdir) = line.rsplit_once(" ").unwrap();
            if let File::Directory { files, .. } = cwd {
                cwd = match files.get_mut(subdir).unwrap() {
                    File::Binary { .. } => panic!("Can't cd to binary"),
                    dir => dir,
                };
            }
        } else if line.starts_with("$ ls") {
            // nothing
        } else if line.starts_with("dir ") {
            let (_, subdir) = line.rsplit_once(" ").unwrap();
            let cwd_pointer: *mut File = cwd;
            if let File::Directory { ref mut files, .. } = cwd {
                files.insert(
                    subdir.to_string(),
                    File::Directory {
                        files: HashMap::new(),
                        parent: NonNull::new(cwd_pointer).unwrap(),
                    },
                );
            }
        } else {
            let (size, name) = line
                .split_once(" ")
                .expect(format!("Invalid ls size string: '{}'", line).as_str());
            if let File::Directory { ref mut files, .. } = cwd {
                files.insert(
                    name.to_string(),
                    File::Binary {
                        bytes: size.parse::<usize>().unwrap(),
                    },
                );
            }
        }
    }

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
