mod aoc_client;

#[derive(Debug)]
enum Error {
    ParseError,
}

fn walk_dir_sizes<'a, I>(lines: &mut I) -> Result<(Vec<usize>, usize), Error>
where
    I: Iterator<Item = &'a str>,
{
    let mut subdir_sizes: Vec<usize> = vec![];
    let mut my_size: usize = 0;
    while let Some(line) = lines.next() {
        if line == "$ cd .." {
            break;
        } else if line.starts_with("$ cd") {
            let (mut sub_sizes, total_subdir) = walk_dir_sizes(lines)?;
            subdir_sizes.append(&mut sub_sizes);
            my_size += total_subdir;
        } else if line.starts_with("$ ls") || line.starts_with("dir ") {
            // nothing
        } else {
            let (size, _name) = line.split_once(" ").ok_or(Error::ParseError)?;
            my_size += size.parse::<usize>().map_err(|_| Error::ParseError)?;
        }
    }
    subdir_sizes.push(my_size);
    return Ok((subdir_sizes, my_size));
}

fn main() {
    let data = aoc_client::input(2022, 7).unwrap();

    let (dir_sizes, total_used) = walk_dir_sizes(&mut data.lines()).unwrap();
    let small_dir_sizes = dir_sizes.iter().filter(|&&d| d <= 100000);
    println!("{}", small_dir_sizes.sum::<usize>());

    let max_space = 70000000;
    let required_free_space = 30000000;
    if total_used < max_space - required_free_space {
        println!("Good news! Enough free space already.");
    } else {
        let required_to_free = total_used - (max_space - required_free_space);
        println!(
            "{}",
            dir_sizes
                .iter()
                .filter(|&&s| s >= required_to_free)
                .min()
                .unwrap(),
        );
    }
}
