use std::num::ParseIntError;
use std::ops::Index;

mod aoc_client;

type Mask = Vec<Vec<bool>>;
struct Trees(Vec<Vec<u8>>);

fn transpose<T: Copy>(vv: &Vec<Vec<T>>) -> Vec<Vec<T>> {
    let width = vv[0].len();
    (0..width)
        .map(|x| vv.iter().map(|v| v[x]).collect())
        .collect()
}

fn interior_reverse<T>(mut vv: Vec<Vec<T>>) -> Vec<Vec<T>> {
    for v in vv.iter_mut() {
        v.reverse();
    }
    vv
}

fn tree_visibility_mask(trees: &Vec<Vec<u8>>) -> Mask {
    trees
        .iter()
        .map(|row| {
            row.iter()
                .scan(None, |acc, &th| match acc {
                    Some(mh) => {
                        let visible = *mh < th;
                        *acc = Some(std::cmp::max(*mh, th));
                        Some(visible)
                    }
                    None => {
                        *acc = Some(th);
                        Some(true)
                    }
                })
                .collect()
        })
        .collect()
}

// I can't figure out how to get reduce to work when the inputs
// are references :/
fn elementwise_or_mask(m1: Mask, m2: Mask) -> Mask {
    m1.iter()
        .zip(m2)
        .map(|(v1, v2)| v1.iter().zip(v2).map(|(&b1, b2)| b1 || b2).collect())
        .collect()
    // .unwrap()
    // .to_vec();
}

impl Trees {
    fn parse(data: &str) -> Result<Trees, ParseIntError> {
        Ok(Trees(
            data.lines()
                .map(|line| {
                    line.chars()
                        .map(|c| c.to_string().parse::<u8>())
                        .collect::<Result<Vec<u8>, _>>()
                })
                .collect::<Result<Vec<Vec<u8>>, _>>()?,
        ))
    }
    fn height(&self) -> usize {
        self.0.len()
    }

    fn width(&self) -> usize {
        self.0[0].len()
    }

    fn visible(&self) -> Mask {
        let left_tree_mask = tree_visibility_mask(&self.0);
        let top_tree_mask = transpose(&tree_visibility_mask(&transpose(&self.0)));
        let right_tree_mask: Mask =
            interior_reverse(tree_visibility_mask(&interior_reverse(self.0.clone())));
        let bottom_tree_mask = transpose(&interior_reverse(tree_visibility_mask(
            &interior_reverse(transpose(&self.0)),
        )));
        // left_tree_mask
        //     .iter()
        //     .for_each(|v| println!("LEFT  : {:?}", v));
        // right_tree_mask
        //     .iter()
        //     .for_each(|v| println!("RIGHT : {:?}", v));
        // top_tree_mask
        //     .iter()
        //     .for_each(|v| println!("TOP   : {:?}", v));
        // bottom_tree_mask
        //     .iter()
        //     .for_each(|v| println!("BOTTOM: {:?}", v));
        let full_tree_mask: Mask = [
            left_tree_mask,
            right_tree_mask,
            top_tree_mask,
            bottom_tree_mask,
        ]
        .iter()
        .cloned()
        // I can't figure out how to use reduce without copying the values passed in :/
        .reduce(elementwise_or_mask)
        .unwrap();
        full_tree_mask
    }

    fn visible_2(&self) -> Mask {
        let mut mask = vec![vec![false; self.height()]; self.width()];
        for x in 0..self.width() {
            mask[0][x] = true;
            mask[self.height() - 1][x] = true;
            let mut max_bottom = self[(x, 0)];
            let mut max_top = self[(x, self.height() - 1)];
            for y in 1..self.height() {
                if self[(x, y)] > max_bottom {
                    // println!("setting {:?} from top (I think)", (x, y));
                    mask[y][x] = true;
                    max_bottom = self[(x, y)];
                }
            }
            for y in (0..self.height() - 1).rev() {
                if self[(x, y)] > max_top {
                    // println!("setting {:?} from bottom (I think)", (x, y));
                    mask[y][x] = true;
                    max_top = self[(x, y)];
                }
            }
        }
        for y in 0..self.height() {
            mask[y][0] = true;
            mask[y][self.width() - 1] = true;
            let mut max_left = self[(0, y)];
            let mut max_right = self[(self.width() - 1, y)];
            for x in 1..self.width() {
                if self[(x, y)] > max_left {
                    // println!("setting {:?} from left", (x, y));
                    mask[y][x] = true;
                    max_left = self[(x, y)];
                }
            }
            for x in (0..self.width() - 1).rev() {
                if self[(x, y)] > max_right {
                    // println!("setting {:?} from right", (x, y));
                    mask[y][x] = true;
                    max_right = self[(x, y)];
                }
            }
        }
        mask
    }

    fn scenic_score(&self, pos: (usize, usize)) -> usize {
        let (x, y) = pos;
        let height = self[(x, y)];
        let shorter = |pos: &(usize, usize)| self[*pos] < height;
        let scan_until_first_taller = |found: &mut bool, point: (usize, usize)| match found {
            false => {
                let h = self[point];
                *found = h >= height;
                Some(h)
            }
            true => None,
        };
        let left_vis = (0..x)
            .rev()
            .map(|x| (x, y))
            .scan(false, scan_until_first_taller)
            .count();
        let right_vis = ((x + 1)..self.width())
            .map(|x| (x, y))
            .scan(false, scan_until_first_taller)
            .count();
        let bottom_vis = (0..y)
            .rev()
            .map(|y| (x, y))
            .scan(false, scan_until_first_taller)
            .count();
        let top_vis = ((y + 1)..self.height())
            .map(|y| (x, y))
            .scan(false, scan_until_first_taller)
            .count();
        // println!("height: {height}");
        // let test = shorter(&(0, 0));
        // println!("shorter?: {test}");
        // println!("left: {left_vis}, right: {right_vis}, top: {top_vis}, bottom: {bottom_vis}");
        // let pp = |p: (usize, usize)| {
        //     let height = self[p];
        //     println!("{p:?} -> {height}");
        // };
        // println!("BOTTOM:");
        // (0..y).rev().map(|y| (x, y)).for_each(pp);
        // println!("TOP:");
        // ((y + 1)..self.height()).map(|y| (x, y)).for_each(pp);
        // println!("LEFT:");
        // (0..x).rev().map(|x| (x, y)).for_each(pp);
        // println!("RIGHT:");
        // ((x + 1)..self.width()).map(|x| (x, y)).for_each(pp);
        left_vis * top_vis * bottom_vis * right_vis
    }
}

impl Index<(usize, usize)> for Trees {
    type Output = u8;
    fn index(&self, pos: (usize, usize)) -> &Self::Output {
        let (x, y) = pos;
        &self.0[y][x]
    }
}

fn main() {
    let data = aoc_client::input(2022, 8).unwrap();

    //     let data = "30373
    // 25512
    // 65332
    // 33549
    // 35390"
    //         .to_string();
    let trees = Trees::parse(data.as_str()).unwrap();
    // trees.0.iter().for_each(|v| println!("{:?}", v));
    // trees.visible().iter().for_each(|v| println!("{:?}", v));
    // trees.visible_2().iter().for_each(|v| println!("{:?}", v));
    // println!("BADGER");
    println!(
        "{}",
        trees
            .visible()
            .iter()
            .flatten()
            .map(|&b| if b { 1 } else { 0 })
            .sum::<usize>()
    );
    println!(
        "{}",
        trees
            .visible_2()
            .iter()
            .flatten()
            .map(|&b| if b { 1 } else { 0 })
            .sum::<usize>()
    );
    let all_points: Vec<(usize, usize)> = (0..trees.width())
        .flat_map(|x| (0..trees.height()).map(move |y| (x, y)))
        .collect();
    // println!("{}", trees.scenic_score((2, 1)));
    let scenic_scores = all_points.iter().map(|&p| trees.scenic_score(p));
    println!("{:?}", scenic_scores.max());
}
