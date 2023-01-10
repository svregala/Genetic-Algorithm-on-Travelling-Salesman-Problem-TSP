/*
Name: Steve Regala
USC Net ID: sregala
ID Number: 7293040280
Homework 1: Genetic Algorithm on Travelling-Salesman Problem (TSP)
CSCI 561: Foundations of Artificial Intelligence
Fall 2022
*/

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.ArrayList;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;

// Main class
public class homework{
   public static void main(String[] args){

      try{
         String fileName = "input3.txt";
         File file = new File(fileName);  // FileNotFoundException is thrown here
         Scanner in = new Scanner(file);

         int num_cities = Integer.parseInt(in.nextLine());
         ArrayList<City> cities = new ArrayList<>(num_cities);

         while(in.hasNextLine()){
            String line = in.nextLine();
            String[] coords = line.split(" ");
            City temp_city = new City(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]));
            cities.add(temp_city);
         }

         ArrayList<ChromosomeVariation> array_of_results = new ArrayList<>();

         for(int i=0; i<10; i++){
            GeneticAlgorithm example_one = new GeneticAlgorithm(cities, num_cities);
            ChromosomeVariation final_result_ONE = example_one.findOptimalPath();
            array_of_results.add(final_result_ONE);
         }

         ChromosomeVariation final_result = find_minimum_path(array_of_results);

         FileWriter result = new FileWriter("output.txt");
         for(int i=0; i<num_cities+1; i++){
            result.write(final_result.chromosome.get(i).x_cord + " " + final_result.chromosome.get(i).y_cord + " " + final_result.chromosome.get(i).z_cord + "\n");
         }
         result.write(String.valueOf(final_result.path_cost));
         result.close();


      }catch(FileNotFoundException e){
         e.printStackTrace();
      }catch (IOException e){
         e.printStackTrace();
      }

   }

   private static ChromosomeVariation find_minimum_path(ArrayList<ChromosomeVariation> result_array){
      ChromosomeVariation ret_value = result_array.get(0);
      for(int i=1; i<result_array.size(); i++){
         System.out.println(result_array.get(i).path_cost);
         if(result_array.get(i).path_cost < ret_value.path_cost){
            ret_value = result_array.get(i);
         }
      }
      return ret_value;
   }
}


// Single city coordinates
class City{
   int x_cord;
   int y_cord;
   int z_cord;

   public City(int x_cord, int y_cord, int z_cord){
      this.x_cord = x_cord;
      this.y_cord = y_cord;
      this.z_cord = z_cord;
   }
}


// Array of cities representing a single permutation
class ChromosomeVariation{

   ArrayList<City> chromosome;
   City start_city;
   int num_cities;
   int path_cost;

   public ChromosomeVariation(ArrayList<City> chromosome_var, City start_city_var, int num_cities_var){
      chromosome = new ArrayList<>(chromosome_var);
      start_city = start_city_var;
      num_cities = num_cities_var;
      path_cost = (int)pathCalculation();
   }

   private double pathCalculation(){
      double euclid_distance=0;
      // to deal with the case that the start city is not included in the chromosome arraylist
      /*if(chromosome.size() == num_cities){
         return euclid_distance;
      }*/
      for(int i=0; i<num_cities; i++){
         euclid_distance += Math.sqrt(Math.pow(chromosome.get(i+1).x_cord - chromosome.get(i).x_cord, 2) + Math.pow(chromosome.get(i+1).y_cord - chromosome.get(i).y_cord, 2) + Math.pow(chromosome.get(i+1).z_cord - chromosome.get(i).z_cord, 2));
      }
      return euclid_distance;
   }

}

// will hold array of permutations, perform parent selection (Roulette), and perform Crossover
class GeneticAlgorithm{

   // Constants
   private final int population_size = 800; // 3000
   private final int mating_size = 300;   // 300
   private final double mutation_probability = 0.5;  // 0.01
   private final int iterations = 200;   // 500

   // object variables
   private ArrayList<City> cities;
   private final int num_city;
   private final int chromosome_size;
   private HashSet<ChromosomeVariation> already_chosen = new HashSet<>();


   public GeneticAlgorithm(ArrayList<City> given_cities, int given_num_city){
      cities = new ArrayList<>(given_cities);
      num_city = given_num_city;
      chromosome_size = given_num_city;
   }

   /*
    * Returns an initial population, or list of paths (a permutation of cities) of size = population_size
    * create an array of Chromosome Variations
    * PART A: INITIAL POPULATION -------------------------------------------------------------------------------------
    * */
   public ArrayList<ChromosomeVariation> createInitialPopulation(){
      ArrayList<ChromosomeVariation> init_population = new ArrayList<>();
      for(int i=0; i<population_size; i++){
         ArrayList<City> temp_chromosome = new ArrayList<>(cities);
         Collections.shuffle(temp_chromosome);
         temp_chromosome.add(temp_chromosome.get(0));    // add start city to last
         ChromosomeVariation final_chromosome = new ChromosomeVariation(temp_chromosome, temp_chromosome.get(0), num_city);
         init_population.add(final_chromosome);
      }
      return init_population;
   }

   // PART B: PARENT SELECTION -------------------------------------------------------------------------------------
   public ArrayList<ChromosomeVariation> createMatingPool(ArrayList<ChromosomeVariation> initial_population){
      ArrayList<ChromosomeVariation> matingPool = new ArrayList<>();
      int sum_path_cost = calculateSumPath(initial_population);
      // Don't choose what is already chosen
      //HashSet<ChromosomeVariation> chosen_already = new HashSet<>();
      for(int i=0; i<mating_size; i++){
         //Collections.shuffle(initial_population);
         matingPool.add(roulette(initial_population, sum_path_cost));
      }
      return matingPool;
   }

   // Helper for createMatingPool function
   private int calculateSumPath(ArrayList<ChromosomeVariation> population){
      int result = 0;
      for(int i=0; i<population.size(); i++){
         result += population.get(i).path_cost;
      }
      return result;
   }

   // Helper for createMatingPool function - roulette selection
   private ChromosomeVariation roulette(ArrayList<ChromosomeVariation> initial_population, int total_path_cost){
      Random rand = new Random();
      int chosen_parent = rand.nextInt(total_path_cost)+1;   // select a random value on roulette wheel (b/w 0 and total_path_cost)

      // ** Lower fitness scores (path_costs) mean better fit chromosomes **
      // considering the least path cost, we must do 1/chosen_parent
      // probability of selecting a chromosome is inversely proportional to its path cost --> smaller fitness means higher probability
      double probability = 1 - ((double)chosen_parent/total_path_cost);
      // suppose probability is S & current_partial_sum is P,
      // starting from the top of initial population, keep adding path costs to partial sum P
      // the chromosome for which P exceeds S is the chosen chromosome
      double current_partial_sum = 0;
      for(ChromosomeVariation elem:initial_population){
         if(!already_chosen.contains(elem)) {
            current_partial_sum += 1 - ((double) elem.path_cost / total_path_cost);
            if(current_partial_sum >= probability){
               already_chosen.add(elem);
               return new ChromosomeVariation(elem.chromosome, elem.start_city, elem.num_cities);   // FIX DIFFERENT OBJECT ISSUE HERE
            }
         }
      }

      // if P does not ever exceed S, then select a parent from original population
      int index_random = rand.nextInt(population_size);
      already_chosen.add(initial_population.get(index_random));
      return new ChromosomeVariation(initial_population.get(index_random).chromosome, initial_population.get(index_random).start_city, initial_population.get(index_random).num_cities);
   }
   /*private ChromosomeVariation roulette(ArrayList<ChromosomeVariation> initial_population, int total_path_cost){
      Random rand = new Random();
      int chosen_parent = rand.nextInt(total_path_cost)+1;   // select a random value on roulette wheel (b/w 0 and total_path_cost)
      // ** Lower fitness scores (path_costs) mean better fit chromosomes **
      // considering the least path cost, we must do 1/chosen_parent
      // probability of selecting a chromosome is inversely proportional to its path cost --> smaller fitness means higher probability
      double probability = (double)1/(double)chosen_parent;
      // suppose probability is S & current_partial_sum is P,
      // starting from the top of initial population, keep adding path costs to partial sum P
      // the chromosome for which P exceeds S is the chosen chromosome
      double current_partial_sum = 0;
      for(ChromosomeVariation elem:initial_population){
         current_partial_sum += ((double)1/(double)elem.path_cost);
         if(current_partial_sum>=probability){
            return new ChromosomeVariation(elem.chromosome, elem.start_city, elem.num_cities);   // FIX DIFFERENT OBJECT ISSUE HERE
         }
      }

      // if P does not ever exceed S, then select a parent from original population
      int index_random = rand.nextInt(population_size);
      return new ChromosomeVariation(initial_population.get(index_random).chromosome, initial_population.get(index_random).start_city, initial_population.get(index_random).num_cities);
   }*/




   // PART C: CROSSOVER -------------------------------------------------------------------------------------
   public ArrayList<ChromosomeVariation> newPopulation(ArrayList<ChromosomeVariation> mating_pool){
      ArrayList<ChromosomeVariation> new_group = new ArrayList<>();
      int curr_group_size = 0;

      while(curr_group_size < population_size){
         ArrayList<ChromosomeVariation> selected_parents = twoRandomParents(mating_pool);
         ArrayList<ChromosomeVariation> new_children = crossover(selected_parents);

         ChromosomeVariation mutation_first_child = possibleMutation(new_children.get(0));
         ChromosomeVariation mutation_second_child = possibleMutation(new_children.get(1));
         new_children.set(0, mutation_first_child);
         new_children.set(1, mutation_second_child);
         new_group.addAll(new_children);

         curr_group_size += 2;
      }

      return new_group;
   }

   private ArrayList<ChromosomeVariation> twoRandomParents(ArrayList<ChromosomeVariation> parent_list){
      Random rand = new Random();
      ArrayList<ChromosomeVariation> result = new ArrayList<>();
      int parent1 = rand.nextInt(parent_list.size());
      int parent2 = rand.nextInt(parent_list.size());
      // eliminate the same 2 parents from breeding
      while(parent1 == parent2){
         parent2 = rand.nextInt(parent_list.size());
      }

      result.add(parent_list.get(parent1)); // parent 1
      result.add(parent_list.get(parent2)); // parent 2
      return result;
   }

   // Using the crossover method: Ordered Crossover
   private ArrayList<ChromosomeVariation> crossover(ArrayList<ChromosomeVariation> parents){
      ArrayList<ChromosomeVariation> child_list = new ArrayList<>();
      Random rand = new Random();
      int first_split = rand.nextInt(chromosome_size);
      int second_split = rand.nextInt(chromosome_size);

      while(first_split==second_split){
         second_split = rand.nextInt(chromosome_size);
      }
      if(first_split>second_split){
         int temp_split = first_split;
         first_split = second_split;
         second_split = temp_split;
      }

      // first & second child
      ArrayList<City> child_one = new ArrayList<>(parents.get(0).chromosome);
      child_one.remove(child_one.size()-1);
      ArrayList<City> child_two = new ArrayList<>(parents.get(1).chromosome);
      child_two.remove(child_two.size()-1);

      // HashSet to keep track of who is already in child_one & child_two
      HashSet<City> child_one_cities = new HashSet<>();
      HashSet<City> child_two_cities = new HashSet<>();

      // first parent & second parent
      ArrayList<City> first_parent = parents.get(0).chromosome;
      first_parent.remove(first_parent.size()-1); // remove the last element in the variation, aka start city

      ArrayList<City> second_parent = parents.get(1).chromosome;
      second_parent.remove(second_parent.size()-1); // remove the last element in the variation, aka start city

      // copy corresponding subsets into children, also update hashsets
      // copy parent 1 subset into child 2, similarly copy parent 2 subset into child 1
      for(int i=first_split; i<=second_split; i++){
         child_one.remove(i); // TEST TEST
         child_one.add(i, second_parent.get(i));
         child_one_cities.add(second_parent.get(i));  // update the cities in the first child

         child_two.remove(i); // TEST TEST
         child_two.add(i, first_parent.get(i));
         child_two_cities.add(first_parent.get(i));   // update the cities in the second child
      }

      // copy the rest of the elements according to ordered crossover
      ArrayList<City> temp_cities_to_add_ONE = new ArrayList<>();
      ArrayList<City> temp_cities_to_add_TWO = new ArrayList<>();
      // copy all the elements starting from one after second_split
      for(int i=second_split+1; i<chromosome_size; i++){
         temp_cities_to_add_ONE.add(first_parent.get(i));
         temp_cities_to_add_TWO.add(second_parent.get(i));
      }
      // copy the rest
      for(int i=0; i<=second_split; i++){
         temp_cities_to_add_ONE.add(first_parent.get(i));
         temp_cities_to_add_TWO.add(second_parent.get(i));
      }

      first_parent.add(first_parent.get(0));
      second_parent.add(second_parent.get(0));


      // TAKE CARE OF THE ** FIRST ** CHILD ---------------
      // add all elements AFTER the second_split
      int temp_count=second_split+1;
      int index=0;
      while(temp_count < chromosome_size && index < temp_cities_to_add_ONE.size()){
         if(!child_one_cities.contains(temp_cities_to_add_ONE.get(index))){
            child_one.remove(temp_count); // TEST TEST
            child_one.add(temp_count, temp_cities_to_add_ONE.get(index));
            child_one_cities.add(temp_cities_to_add_ONE.get(index));

            temp_count++;
         }
         index++;
      }
      // add all elements BEFORE the first_split
      temp_count=0;
      while(temp_count < first_split && index < temp_cities_to_add_ONE.size()){
         if(!child_one_cities.contains(temp_cities_to_add_ONE.get(index))){
            child_one.remove(temp_count); // TEST TEST
            child_one.add(temp_count, temp_cities_to_add_ONE.get(index));
            child_one_cities.add(temp_cities_to_add_ONE.get(index));

            temp_count++;
         }
         index++;
      }

      child_one.add(child_one.get(0));
      child_list.add(new ChromosomeVariation(child_one, child_one.get(0), parents.get(0).num_cities));

      // TAKE CARE OF THE ** SECOND ** CHILD ----------------
      // add all elements AFTER the second_split
      temp_count=second_split+1;
      index=0;
      while(temp_count < chromosome_size && index < temp_cities_to_add_TWO.size()){
         if(!child_two_cities.contains(temp_cities_to_add_TWO.get(index))){
            child_two.remove(temp_count); // TEST TEST
            child_two.add(temp_count, temp_cities_to_add_TWO.get(index));
            child_two_cities.add(temp_cities_to_add_TWO.get(index));

            temp_count++;
         }
         index++;
      }
      // add all elements BEFORE the first_split
      temp_count=0;
      while(temp_count < first_split && index < temp_cities_to_add_TWO.size()){
         if(!child_two_cities.contains(temp_cities_to_add_TWO.get(index))){
            child_two.remove(temp_count); // TEST TEST
            child_two.add(temp_count, temp_cities_to_add_TWO.get(index));
            child_two_cities.add(temp_cities_to_add_TWO.get(index));

            temp_count++;
         }
         index++;
      }

      child_two.add(child_two.get(0));
      child_list.add(new ChromosomeVariation(child_two, child_two.get(0), parents.get(1).num_cities));

      return child_list;
   }


   private ChromosomeVariation possibleMutation(ChromosomeVariation child){
      Random rand = new Random();
      double value = rand.nextDouble();

      if(value<mutation_probability){
         child.chromosome.remove(child.chromosome.size()-1); // prevent from swapping first and last elements (both the same)
         Collections.swap(child.chromosome, rand.nextInt(num_city), rand.nextInt(num_city));
         child.chromosome.add(child.chromosome.get(0)); // add in the new first element, in the case that it got swapped
         return new ChromosomeVariation(child.chromosome, child.chromosome.get(0), child.num_cities);
      }
      return child;
   }


   public ChromosomeVariation findOptimalPath(){
      ArrayList<ChromosomeVariation> overall_pool = createInitialPopulation();
      ChromosomeVariation ret_value = overall_pool.get(0);

      for(int i=0; i<iterations; i++){
         already_chosen.clear();
         ArrayList<ChromosomeVariation> mating_pool = createMatingPool(overall_pool);
         overall_pool = newPopulation(mating_pool);
         ret_value = minimumCostPath(overall_pool);
      }
      return ret_value;
   }

   private ChromosomeVariation minimumCostPath(ArrayList<ChromosomeVariation> our_list){
      ChromosomeVariation result = our_list.get(0);
      for(int i=1; i<our_list.size(); i++){
         if(our_list.get(i).path_cost < result.path_cost){
            result = our_list.get(i);
         }
      }
      return result;
   }

   // TESTING PURPOSES - Print initial population
   public void printArrayPerm(){
      ArrayList<ChromosomeVariation> initial_pop = createInitialPopulation();
      for(int i=0; i<20; i++){
         System.out.println("Permutation #: " + i);
         for(int j=0; j<initial_pop.get(i).chromosome.size(); j++){
            System.out.println(initial_pop.get(i).chromosome.get(j).x_cord + " , " + initial_pop.get(i).chromosome.get(j).y_cord + " , " + initial_pop.get(i).chromosome.get(j).z_cord);
         }
         System.out.println("Euclidean Distance = " + initial_pop.get(i).path_cost);
         System.out.println();
      }
   }

}


// Using the crossover method: Partially Mapped Crossover
   /*private ArrayList<ChromosomeVariation> crossover(ArrayList<ChromosomeVariation> parents){
      Random rand = new Random();
      int split = rand.nextInt(chromosome_size);
      ArrayList<ChromosomeVariation> child_list = new ArrayList<>();

      // first parent
      ArrayList<City> first_temp_city = new ArrayList<>(parents.get(0).chromosome);
      first_temp_city.remove(first_temp_city.size()-1);  // remove the last element in the variation, aka start city
      ChromosomeVariation first_parent = new ChromosomeVariation(first_temp_city, parents.get(0).start_city, parents.get(0).num_cities);
      ChromosomeVariation first_parent_COPY = new ChromosomeVariation(first_temp_city, parents.get(0).start_city, parents.get(0).num_cities);

      // second parent
      ArrayList<City> second_temp_city = new ArrayList<>(parents.get(1).chromosome);
      second_temp_city.remove(second_temp_city.size()-1);   // remove the last element in the variation, aka start city
      ChromosomeVariation second_parent = new ChromosomeVariation(second_temp_city, parents.get(1).start_city, parents.get(1).num_cities);

      // first child
      for(int i=0; i<split; i++){
         City elem = second_parent.chromosome.get(i);
         Collections.swap(first_parent.chromosome, i, first_parent.chromosome.indexOf(elem));
      }
      first_parent.chromosome.add(first_parent.chromosome.get(0));   // add the first city back in
      child_list.add(new ChromosomeVariation(first_parent.chromosome, first_parent.chromosome.get(0), first_parent.num_cities));

      // second child
      for(int j=split; j<chromosome_size; j++){
         City elem = first_parent_COPY.chromosome.get(j);
         Collections.swap(second_parent.chromosome, j, second_parent.chromosome.indexOf(elem));
      }
      second_parent.chromosome.add(second_parent.chromosome.get(0));
      child_list.add(new ChromosomeVariation(second_parent.chromosome, second_parent.chromosome.get(0), second_parent.num_cities));

      return child_list;
   }*/
