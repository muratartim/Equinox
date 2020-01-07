#! /opt/perl5/bin/perl -w           #####################          #! /opt/perl5/bin/perl -w

#use Tk;
use strict;
use POSIX qw(sin cos ceil floor log10 atan);   # Generates Reduced ANA File  based on Rforth Reduction!
#use Tk::ObjScanner;

print STDERR "  *  Generates REDUCED ANA & STH File! Method based on Rfort-Extended Process [X022RP1411550]\n";
print STDERR "  *  No Limitations to Filesizes! Including Extreme Detailed Reporting Capabilities! \n";
print STDERR "  *  Multiple *.stf Files Capability - No Limitations to number of Pilot Points!\n";
print STDERR "  *  Including: AutoRun STH/RFORTH/MINI/ANA. Analyses with 1-D or 2-D with Stress Rotation!\n";
print STDERR "  *  Incl. Improved ISAMI format for ANA -> Incl. Improved Maxdam NL code Compatibility!\n";
print STDERR "  *  RFORTH EXTENDED method!  Inspired by Yudi!\n";
print STDERR "  *  From v2.4 upwards Global InputFile required with all Input Variables!\n";
print STDERR "  *  From v2.6 upwards additional user-input: 'attack_only_these_flights' (i.e. only flights listed will be affected)!\n";
print STDERR "  *  From v2.8 upwards additional user-input: Accepts additional Factors e.g. Load to Stress per Pilot Point individually!\n";
print STDERR "  *  From v3.1 if only 1 STF file is used, then reduced ANA will use name of STF - not name of orig ANA!\n";
print STDERR "  *  From v3.2 Option to Run individually per PP even where multiple PPs are provided. Variable: run_each_STF_separately\n";
print STDERR "  *  From v3.2 Option to Detect & Run Automatically flights with Peaks above a certain User-Input-peaks. Variable: attack_flights_above_peaks\n";
print STDERR "  *  From v3.4 Option to Include: Lower_Truncation_Value and 1g_offset_value per PP!\n";
print STDERR "  *  From v3.6 Accepts \$ARGV[0] from Command Line !\n";

my $self        = {};   

$self->{global_inputfilename}        = $ARGV[0];
$self->{run_only_till_flight_num}    = $ARGV[1]; # Default is 1e6 to capture all flights!
$self->{run_each_STF_separately}     =    0;     # Default 0; If > 0 it forces all STF to be run separately even with Multiple STFs in Global Input
$self->{attack_flights_above_peaks}  =    0;     # Default Set to 0; Activate if using ALL in Global Input but you need to Target Big Flights without listing them! Input is peaks e.g. if 99000, then flights with peaks above this value will be attacked!
$self->{tool_version}                =  3.6;

# if (defined $ARGV[0])  {$self->{global_inputfilename}    = $$ARGV[0]; print STDERR "using inputfile: $ARGV[0]\n";} # Accepts \$ARGV[0] from Command Line

#______________________________Important NOTES__________________________________________________________
#RFORT[Mpa]              - Expected as Mpa or KN after all factors have been applied!
#FACTOR LOAD to STRESS   - The Values in STF will be multiplied by this Value & Overall_factor!     
#1G_STEADY_OFFSET        - This Value will be added to 1gs in STF before other factors are applied! Thus should be in same unit as STF!   
#LOWER_TRUNCATION_VALUE  - This should be in same unit as STF!    Or set to minus infinity if you do not use it e.g. -1e9 

##  This is an Example of INPUTFILE for RFORTH Extended Tool a.k.a  Level 3 & 4 Test Spectra Reduction Tool
# begin GLOBAL_INPUTS

             # job_name                 =   S19_MT
             # date                     =   01-04-2015
			 # pilot_points_directory   =   PP_S19
             # index_file               =   A350XWB1000TL-111S19-MT2.txt
             # flugablauf_file          =   A350XWB1000TL-111S19-MT2.ana
			 # add2sth_Delta_P          =      1
			 # reference_Delta_P        = 1327.0
			 # enable_SLOG_mode         =      0
			 # rotation_degrees         =      0
			 # use_stress_column        =      1
			 # set_it_zero              =     NO
			 # fatigue_fac_all          =    1.0
			 # delta_p_fac_only         =    1.0
	    # attack_only_these_flights     =    ALL        

# end	GLOBAL_INPUTS		 

# begin PILOT_POINTS	
# #  Note: The FACTOR LOAD to STRESS is applied first to the data THEN RFORT value is applied - thus RFORT is expected as Mpa!
# #  RFORT[Mpa]                PILOT POINT FILENAME                 FACTOR LOAD to STRESS      1G_STEADY_OFFSET       LOWER_TRUNCATION_VALUE
    # 3.0      =                 PP01_S19_MT.stf                   =         1.00           =         1.00           =         1.00   # Example using Stress!   
    # 5.0      = PP01FR95_04-05_B_Loc6_L_19552028_Y_P_MX_2D_P.stf  =         0.01           =         1138           =         1138   # Example using LOADS!
# end PILOT_POINTS		 

#  Example INPUTS explanations (Note: Some of these options can only be activated in this Tool itself)
#  attack_only_these_flights = ALL  (default) or the Flight Numbers (e.g. 2,4,5) comma separated!
#  keep_these_events_always  = NONE (default) or exact name as in TXT e.g. EXC_LMLG_F2, EXC_GXTI_F2 (NOT IMPLEMENTED due to Performance reasons). User can use High Stresses for Events they want to keep!
#  RFORTH_value             =   20.0;     # Can differ from one PP to another. Should be same unit as in STF files after Factor is applied! Mpa or KN or ?
#  add2sth_Delta_P          =      0;     # 0 or 1 to use DP or not?
#  reference_Delta_P        = 1327.0;     # DP reference value (see conversion table for details)
#  reference_Delta_T        =    0.0;     # Ref. Delta Temperature value
#  enable_SLOG_mode         =      0;     # Total Report Use for Debugging Only using 1D XXXXXXXXXXXXXX values!!!!!!!!
#  esg_on                   =      0;     # remove additional column for LR ESG only!
#  rotation_degrees         =      0;     # degrees - rotation angle?  Use 0� for 1-D Spectra Inputs 
#  use_stress_column        =      1;     # default is 0.  Can be Fixed for 1, 2, or 3rd Only Column!  However does not Allow Angle Rotation if  >  0 !!!!!!!!!!!!!!
#  set_it_zero              =   'NO';     # If YES/NO? (Yes will delete all negative Values from Spectra)
#  fatigue_fac_all          =    1.0;     # Multiply all Stresses with this Factor (also DP)!;
#  delta_p_fac_only         =    1.0;     # Multiply Factor applied Only on DP value.
#  high_accuracy_of_results =      0;     # more digits  after the comma are printed in report file
#  warn_if_li_nl_cases_comb =      0;     # warns if > 0 that Linear and NonLinear cases are combined 
#  run_only_till_flight_num =    1e6;     # default is 1e9  1e6! Yes really no Limit!  
###################################### Do not Edit below this Line ##############################################################

open(INPUT,  "<". $self->{global_inputfilename}) or print STDERR "  *** Inputfile not Found |$self->{global_inputfilename}|\n";
my @input = <INPUT>;
chop(@input);
close(INPUT);

my @rforthfiles =  (); my @rforthvalue =  ();   my $type   = 'NA';  my $i = 0;   my @factorvalue =  (); my @steadyoffsetvalue =  (); my @lowertruncationvalue =  ();

foreach (@input)
  {
	if (($_ =~m/^\s*$/) || ($_ =~m/\#/)) {next;}

	if ($_ =~m/begin\s*GLOBAL_INPUTS/i) {$type = 'GLOBAL'; next;}
	if ($_ =~m/end\s*GLOBAL_INPUTS/i)   {$type = 'NA';     next;}
	if ($_ =~m/begin\s*PILOT_POINTS/i)  {$type = 'PILOTS'; next;}
	if ($_ =~m/end\s*PILOT_POINTS/i)    {$type = 'NA';     next;}	
	
	if ($type eq 'GLOBAL') 
       {
	      my ($key, $value) = split('\s*=\s*', $_);
	      if((defined $key) && (defined $value))
	         {
	            $key                     =~s/\s*//g;
	            $value                   =~s/\s*//g;
				$self->{$key}            = $value;
	        }		  
       }	   
	if ($type eq 'PILOTS') 
       {
	      my ($key, $value, $factor, $steady_offset, $lower_truncation_value) = split('\s*=\s*', $_);
	      if((defined $key) && (defined $value))
	         {
	            $key                     =~s/\s*//g;
	            $value                   =~s/\s*//g;
				push(@rforthvalue, $key);
				push(@rforthfiles, $value);
	         }
	      if((defined $factor) && (defined $key))
	         {
	            $factor                  =~s/\s*//g;
				push(@factorvalue, $factor);
	         } else {push(@factorvalue, '1.0');}	     # Default Factor is 1.0!	

	      if((defined $steady_offset) && (defined $factor))
	         {
	            $steady_offset           =~s/\s*//g;
				push(@steadyoffsetvalue, $steady_offset);
	         } else {push(@steadyoffsetvalue, '0.0');}	 # Default Offset is 0.0!	
	      if((defined $lower_truncation_value) && (defined $steady_offset))
	         {
	            $lower_truncation_value  =~s/\s*//g;
				push(@lowertruncationvalue, $lower_truncation_value);
	         } else {push(@lowertruncationvalue, '-9876543210.0');}	 # Default Lower TRunc Value is -9876543210.0!	
       }	  	     
  }

my @stressfiles = ();

if (defined $self->{pilot_points_directory})
  {
     foreach(@rforthfiles)
	  {
        my $file = $self->{pilot_points_directory} . '/' . $_;
		push(@stressfiles, $file);
      }
  } else {print STDERR " *** Global Variables not defined for  pilot_points_directory\n";}

  
if (defined $self->{attack_only_these_flights})
  {
     my @a = split('\s*,\s*', $self->{attack_only_these_flights});
	 foreach (@a) {$self->{attack_only_these_flights_numbers}->{$_} = 1;}
  }  
if (defined $self->{keep_these_events_always})
  {
     my @a = split('\s*,\s*', $self->{keep_these_events_always});
	 foreach (@a) {$self->{keep_these_events_always_names}->{$_} = 1;}
  }  

@{$self->{USER}->{PPFILES}}          = @stressfiles;
@{$self->{USER}->{RFORTH}}           = @rforthvalue;
@{$self->{USER}->{FAC_PP}}           = @factorvalue;
@{$self->{USER}->{GG_OFFSET}}        = @steadyoffsetvalue;  
@{$self->{USER}->{LOWER_TRUNCATION}} = @lowertruncationvalue;

@{$self->{load_these_missions_only}} = ('1','2','3','4','5','6','7','8','9',); # only these missions will get loads
$self->{reference_Delta_T}           =     0.0;     # Ref. Delta Temperature value
$self->{esg_on}                      =       0;     # remove additional column for LR ESG only!
$self->{high_accuracy_of_results}    =       0;     # more digits  after the comma are printed in report file
$self->{warn_if_li_nl_cases_comb}    =       0;     # warns if > 0 that Linear and NonLinear cases are combined 
unless (defined $self->{attack_only_these_flights}) {$self->{attack_only_these_flights}  =  ' ALL';}     # To secure default values & backwards compatibility!  
unless (defined $self->{keep_these_events_always})  {$self->{keep_these_events_always}   =  'NONE';}     # To secure default values & backwards compatibility!  

foreach ('job_name','add2sth_Delta_P','reference_Delta_P','enable_SLOG_mode','rotation_degrees','use_stress_column','set_it_zero','fatigue_fac_all','delta_p_fac_only')     {unless (defined $self->{$_}) {print STDERR " *** not defined global variable |$_|\n"; return;}}

if ($self->{add2sth_Delta_P} < 1) {$self->{reference_Delta_P} = 9876543219;}
@{$self->{mini_files}}   = ();              

foreach (@stressfiles)
  {
    $self->{stress_input_file}         = $_;
	$self->{RFORTH_value}              = shift(@rforthvalue);
	$self->{PP_factor}                 = shift(@factorvalue);
	$self->{PP_steady_offset}          = shift(@steadyoffsetvalue);  
	$self->{PP_lower_truncation_value} = shift(@lowertruncationvalue);
    $self->{eid}                       = $self->{stress_input_file};
    $self->{eid}                       =~s/\..+$//;
    $self->{complete_validity}         = 0;
    $self->{max_stress_value}          = -1e10;
    $self->{min_stress_value}          =  1e10;
    $self->{flight_max_value}          =  0;
    $self->{flight_min_value}          =  0;
    $self->{switch_off_D2_error}       =  1;     # Do not Switch off (Leave it = 0) unless you know what you are doing!	
    unless ($self->{rotation_degrees} =~m/^\s*\d+\s*$/) {print STDERR " *** Rotation Angle required! For 1D use 0 degrees!"; return;}
	
    $self->{angle_radian}             = $self->{rotation_degrees} * 3.142 / 180.0;
    $self->{STEADY}                   = {};
    $self->{issy_unique_numbers}      = {};
    $self->{LCASE}                    = {};
    $self->{DPCASE}                   = {};
    $self->{PROFILE}                  = {};
    unless ((-e $self->{index_file}) && (-e $self->{flugablauf_file})) {print STDERR " *** Rejected |$_| Not all Files Exists!\n"; next;}
    &get_information_and_run($self);
	if ($self->{run_each_STF_separately} > 0) 
	  {
	    &write_COMPLETED_FLUGABALAUF_file_USER_requested_SINGLE($self);
	  }
    #last;
  }
if ($self->{run_each_STF_separately} < 1) {&write_COMPLETED_FLUGABALAUF_file_USER_requested_ALL($self);}
print STDERR "  * Process Completed!\n";
#&activate_objscan($self);
#MainLoop;




sub get_information_and_run
  {
    my $self   = shift;

    my $sth_file  = $self->{eid} .  '_EE.sth';
    my $sth_rft   = $self->{eid} .  '_EE.rft';
    my $report    = $self->{eid} .  '_EE.rep';
    my $log       = $self->{eid} .  '_EE.log';
    my $mini_ana  = $self->{eid} .  '.mini';
    my $mini_log  = $self->{eid} .  '.mini.log';
	$self->{single_mini_file}  = $mini_log;
    push(@{$self->{mini_files}}, $mini_log);
    
    @{$self->{list_of_NL_cases}} = ();

    open(OUTPUT,  ">" .$sth_file);
    open(REPORT,  ">" .$report);
    open(LOG,     ">" .$log);	
    open(RFORTOUT,">" .$sth_rft);
    open(CLASSNEW,">" .$mini_ana);	
    open(CLASSLOG,">" .$mini_log);	
	
    print STDERR "  *  attempting to create STH to:  \= $sth_file\n";
    
    print OUTPUT   " Spectra Generated by: Generate_RFORTH_CLASS_Module_STF_concept_Multiple_X $self->{tool_version}\n";
    print OUTPUT   " CDF:|$self->{flugablauf_file}| CVT:|$self->{index_file}| STF:|$self->{stress_input_file}| StressColumn: $self->{use_stress_column}\n";
    print OUTPUT   " 1-D Spectra - Spectra Checks before Delivery!  F_O: $self->{fatigue_fac_all} DP: $self->{delta_p_fac_only} Angle: $self->{rotation_degrees}  PP_Factor: $self->{PP_factor}\n";
    print OUTPUT   " Calculation step:  1 \n";

    print RFORTOUT " Spectra Generated by: Generate_RFORTH_CLASS_Module_STF_concept_Multiple_X $self->{tool_version}\n";
    print RFORTOUT " CDF:|$self->{flugablauf_file}| CVT:|$self->{index_file}| STF:|$self->{stress_input_file}| StressColumn: $self->{use_stress_column}\n";
    print RFORTOUT " 1-D Spectra - Spectra Checks before Delivery!  F_O: $self->{fatigue_fac_all} DP: $self->{delta_p_fac_only} Angle: $self->{rotation_degrees}  PP_Factor: $self->{PP_factor}\n";
    print RFORTOUT " Calculation step:  1  RFORTH OUTPUT FILE [$self->{RFORTH_value}]\n";    
    
    print REPORT " +++++++++ STH derivation ++++++++++++++++ \n";
	
    &load_loadcase_data_into_structure($self);
    &load_stress_input_eid_data($self);
    #last;
	
    &load_one_FT_Block_from_flugabluaf_and_process($self);
    print LOG "  * spectra validity: $self->{complete_validity} \n";
    print LOG "  * max_value: $self->{max_stress_value} Flight_Num: $self->{flight_max_value}\n";
    print LOG "  * min_value: $self->{min_stress_value} Flight_Num: $self->{flight_min_value}\n";

    foreach (@{$self->{list_of_NL_cases}})
      {
	my $a = unpack('A4', $_);
	$self->{unique_nl_cases}->{$a} = 1;
      }

    print LOG "NL Case List: ";
    foreach (sort keys %{$self->{unique_nl_cases}})
      {
	print LOG "$_ ";
      }
    print LOG "\n";

    close(LOG);
    close(OUTPUT);
    close(REPORT);
	close(RFORTOUT);
    close(CLASSNEW);	
    close(CLASSLOG);	
  }

  

sub load_one_FT_Block_from_flugabluaf_and_process
  {
    my $self              = shift;
    my $root              = shift;
    my $file              = $self->{flugablauf_file};
    print STDERR "  *  generating sth data for eid:  \= $self->{eid}\n";

    open(FLUGAB,  "<" . $file);
    my $type   = 0;
    my $no     = -1;
    my $jump   = 0;

    while (<FLUGAB>)
      {
	chop($_);
	if (($_ =~m/^\s*$/) || ($_ =~m/^\s*\#/)) {print REPORT "$_\n"; print CLASSNEW sprintf("%-70s %8s%s","$_","MiNiZeD","\n"); next;}

	my $line = $_;
	$line    =~s/^\s*//;

	if ($line =~m/^TF/)
	  {
	    $no++;
	    $type   = 1;

	    if ($no > 0)
	      {
             if ($no > $self->{run_only_till_flight_num}) {$jump = 1; last;};
	         &process_the_current_flugablauf_block($self,$no);
	      }
	    $self->{BLOCK}    = {};
	    $self->{BLOCK}->{HEAD} = $line;
	    next;
	  }
	elsif ($type > 2)
	  {
	    push(@{$self->{BLOCK}->{DATA}}, $line);
	    next;
	  }
	elsif ($type == 1)
	  {
	    $type++;
	    $self->{BLOCK}->{POINTS} = $line;
	    next;
	  }
	elsif ($type == 2)
	  {
	    $type++;
	    my ($valid, $block) = split('\s+', $line);
	    $self->{BLOCK}->{VALID} = $valid;
	    $self->{BLOCK}->{BLOCK} = $block;
	    next;	
	  }
      }
    $no++;
    &process_the_current_flugablauf_block($self,$no) unless($jump > 0);
    close(FLUGAB);
    $self->{total_flights} = $no;
    print STDERR "\n\n";
  }



sub process_the_current_flugablauf_block
  {
    my $self     = shift;
    my $num      = shift;
    my $eid      = $self->{eid};
    my $i        = 0;

    my $points = $self->{BLOCK}->{POINTS};
    my $valid  = $self->{BLOCK}->{VALID};
    my $block  = $self->{BLOCK}->{BLOCK};
    my @data   = @{$self->{BLOCK}->{DATA}};
    my $real_points = $#data +1;
	
	if (($self->{attack_only_these_flights} =~m/ALL/i) && ($points > $self->{attack_flights_above_peaks})) {$self->{attack_only_these_flights_numbers}->{$num} = 1;}
	
    $self->{BLOCK}->{HEAD} =~m/TF_(.+)\s*\(/;
    my $header = $1;

    $self->{complete_validity} = $self->{complete_validity} + $valid * $block;
    print LOG sprintf("TF:%5i   valid:%5i   block:%3i   Points:%8i %s", "$num","$valid","$block","$points","\n");
	
    if ($real_points != $points) 
      {
	print LOG " *** ERROR: TFnum: $num  Points_in_File: $points ! Use_Real_Points: $real_points \n";
      }

    print OUTPUT sprintf("%10.2f%10.2f %s",  "$valid","$block","\n");
    print OUTPUT sprintf("%10i%62s%-10s%s",  "$points","","$header","\n");
    print REPORT sprintf("%10i%5i %-100s%s", "$points","$num","$self->{BLOCK}->{HEAD}","\n");
    print STDERR " TF_$num \n";
	
	@{$self->{BLOCK_STRESSES}->{DATA}} = ();

    foreach (@data)
      {
		my $line = $_;
		$line    =~s/^\s*//;
		my ($code,$coco,$dp,$temp) = split('\s+', $line);
		$self->{stress_value} = 0;
		&analyze_coco_fourteen_position($self, $coco, $dp, $temp);
		$i++;
		# stress in MPa
		if(($self->{stress_value} < 0) && ($self->{set_it_zero} =~m/YES/))
		  {
		    $self->{stress_value} = 0;
		  }
		print OUTPUT sprintf("%10.2f", "$self->{stress_value}") if ($self->{high_accuracy_of_results} < 1);  
		print OUTPUT sprintf("%10.4f", "$self->{stress_value}") if ($self->{high_accuracy_of_results} > 0);
		
		if ($i == 8) {$i  = 0;	print OUTPUT "\n";}

		if ($self->{stress_value} > $self->{max_stress_value})
		  {
		    $self->{max_stress_value} = $self->{stress_value};
		    $self->{flight_max_value} = $num;
		  }
		if ($self->{stress_value} < $self->{min_stress_value})
		  {
		    $self->{min_stress_value} = $self->{stress_value};
		    $self->{flight_min_value} = $num;
		  }
		if ($self->{stress_value} < $self->{PP_lower_truncation_value}) {$self->{stress_value} = $self->{PP_lower_truncation_value};}  # Lower Truncation Used Here!  
		if ($self->{enable_SLOG_mode} > 0) 
		  {
		    print REPORT sprintf("%12.2f %16s %-30s%s","$self->{stress_value}","$coco","$self->{long_list_loadcases}","\n")
		  }
		push (@{$self->{BLOCK_STRESSES}->{DATA}}, $self->{stress_value});  
      }
    unless ($i == 0) {$i  = 0; print OUTPUT "\n";}
	
    &rfort_sort_and_organize_this_flightblock($self, $num);  
    &write_the_updated_rforth_CLASS_version($self, $num);	
  }

  
  
sub analyze_coco_fourteen_position
  {
    my $self   = shift;
    my $coco   = shift;
    my $dp     = shift;
    my $temp   = shift;
    my $eid    = $self->{eid};
    my $angle  = $self->{angle_radian};       # radians expected!
    my @a      = ();
    my $PSTEP  = 1;
	my $CSTEP  = 1;

    $coco      =~m/(\d\d\d\d)(\d\d)(\d\d)(\d\d)(\d\d)(\d\d)/;

    $a[0]      =  $1;
    $a[1]      =  $2;
    $a[2]      =  $3;
    $a[3]      =  $4;
    $a[4]      =  $5;
    $a[5]      =  $6;
    $dp        =  $dp * $self->{delta_p_fac_only}; # only on DP Value
    	
    if ((length($coco) < 14) || (length($coco) > 14))
      {
        print REPORT "\n *** Error |$coco| not standard length!\n";
        print LOG    "\n *** Error |$coco| not standard length!\n";
      }

    my $code     = shift(@a);
    my $gg_code  = $code . '0';
    my $issy_gg  = $self->{STEADY}->{$gg_code};
    my $x        = $self->{$eid}->{$issy_gg}->{X};  my $x_1g = $x;
    my $y        = $self->{$eid}->{$issy_gg}->{Y};
    my $xy       = $self->{$eid}->{$issy_gg}->{XY};
    my $i        = 0;

    unless (defined $self->{STEADY}->{$gg_code}) {print LOG " *** cannot inteprete: $code for $coco\n"; $self->{stress_value} = 99999999; return;}
    my $counter  = 0;
    my $ex_1     = 0;
    my $ex_2     = 0;

    my $mission  = unpack('A1 A4', $code);

	my $dp_x     = 0; my $dp_y     = 0; my $dp_xy    = 0;
	if ($self->{add2sth_Delta_P} > 0)
	  {
        my $issy_dp  = $self->{DPCASE}->{$mission};	  
        $dp_x     = $self->{$eid}->{$issy_dp}->{X}  * $dp / $self->{reference_Delta_P};
        $dp_y     = $self->{$eid}->{$issy_dp}->{Y}  * $dp / $self->{reference_Delta_P};
        $dp_xy    = $self->{$eid}->{$issy_dp}->{XY} * $dp / $self->{reference_Delta_P};
        unless (defined $self->{$eid}->{$issy_dp}->{X}) {print LOG " *** cannot inteprete: $code for $coco\n"; $self->{stress_value} = 99999999; return;}		
      }
    $x  = $x  + $dp_x;
    $y  = $y  + $dp_y;
    $xy = $xy + $dp_xy;
    my $dp_val   = 0.5*($dp_x+$dp_y) + 0.5*($dp_x-$dp_y)*cos(2*$angle) + $dp_xy*sin(2*$angle); 		
	
    my $list_segnames = sprintf("%21s[%6.1f]","$self->{LCASE}->{$gg_code}->{segname}->{1}","$x_1g");
    $list_segnames    = $list_segnames . ' '  . sprintf("[DP%6.1f %6.2f]","$dp","$dp_x");
    #print STDERR " DP value: $dp   $coco\n";
    #print REPORT "$coco  lcnum: $issy_gg v: $x\n";

    foreach (@a)
      {
	$i++;
	my $ip        = $_;
	my ($n, $d)   = unpack('A1A1', $ip);
	my $incre     = $code . $i;
	my ($x_i,$y_i,$xy_i) = 0;

	if ($ip =~m/00/) {$ex_2++; next;}

	if (defined $self->{LCASE}->{$incre}->{issyno}->{$d})
	  {
	    my $issy = $self->{LCASE}->{$incre}->{issyno}->{$d};
	    my $fac  = @{$self->{LCASE}->{$incre}->{fac}->{$d}}[$n-1];
	    unless (defined $self->{LCASE}->{$incre}->{issyno}->{$d}) {print LOG " *** Loadcase Number not Found: $issy\n"; $self->{stress_value} = 99999999; return;}
	    $x_i     = $self->{$eid}->{$issy}->{X}  * $fac;
	    $y_i     = $self->{$eid}->{$issy}->{Y}  * $fac;
	    $xy_i    = $self->{$eid}->{$issy}->{XY} * $fac;
	    $ex_1++;
	    $x       = $x  + $x_i;
	    $y       = $y  + $y_i;
	    $xy      = $xy + $xy_i;
	    $counter++; 

	    my $name = $self->{LCASE}->{$incre}->{segname}->{$d};
	    $list_segnames = $list_segnames . ' ' .$name . sprintf("[%6.3fx%-6.1f%8.2f]","$fac","$self->{$eid}->{$issy}->{X}","$x_i");

	    my $seefac = abs($fac);
	    if ($seefac > 8) {print LOG sprintf("%24s %8s %20s %10s%s"," ** WARN CVT  L Factor: ","$fac","$coco","$incre","\n");}
	    #print REPORT "     LC1 <lcnum: $issy>  v: $x_i fac: $fac \n";
	  }

	elsif (($d == 2) && (!defined $self->{LCASE}->{$incre}->{issyno}->{2}))
	  {
	    if (defined $self->{LCASE}->{$incre}->{issyno}->{1}) 
	      {
		my $issy = $self->{LCASE}->{$incre}->{issyno}->{1};
		my $fac  = @{$self->{LCASE}->{$incre}->{fac}->{1}}[$n-1] * -1.0;

		$x_i     = $self->{$eid}->{$issy}->{X}  * $fac;
		$y_i     = $self->{$eid}->{$issy}->{Y}  * $fac;
		$xy_i    = $self->{$eid}->{$issy}->{XY} * $fac;
		$ex_1++;
		$x       = $x  + $x_i;
		$y       = $y  + $y_i;
		$xy      = $xy + $xy_i;
		$counter++;

		my $name = $self->{LCASE}->{$incre}->{segname}->{1};
		$list_segnames = $list_segnames . ' ' .$name . sprintf("[%6.3fx%-6.1f%8.2f]","$fac","$self->{$eid}->{$issy}->{X}","$x_i");

		my $seefac = abs($fac);
		if ($seefac > 8) {print LOG sprintf("%24s %8s %20s %10s%s"," ** WARN CVT  L Factor: ","$fac","$coco","$incre","\n");}
		#print REPORT "     LC2 <lcnum: $issy>  v: $x_i fac: $fac\n";
	      }
	  }

	my $nl_incre = $code . $i . $ip;  my $nl_core = $code . $i;
	
	if (defined $self->{LCASE}->{$nl_incre}->{issyno}->{1})
	  {
	    my $issy = $self->{LCASE}->{$nl_incre}->{issyno}->{1};
	    my $fac  = @{$self->{LCASE}->{$nl_incre}->{fac}->{1}}[0];
	    #print REPORT "    NLC $coco <$nl_incre> <$issy> <$fac>\n";

	    if ($ip =~m/2$/) # Maxdam adds a -1 again to NLC of the 2nd direction!!!!!!!
	      {
       unless (defined  $self->{$eid}->{$issy}->{X})
       {print LOG " *** Undefined ISSYnum |$coco|GG:$code|ISSY:$issy|DerivedNLCODE:$nl_incre|\n"; $self->{$eid}->{$issy}->{X} = 9876543210;}	
	    my $cifothernlc   =  0;
		my $max_extra_fac = -1;
        for	(1 .. 9) { my $nli = $nl_core . $_;  if(defined $self->{LCASE}->{$nli}->{mission} ) {$cifothernlc++;} }  # see MAXDAM rules for NL codes intepretation email sent 21/11/2011
		if ($cifothernlc > 2) {$max_extra_fac = 1;}
		$x_i     = $self->{$eid}->{$issy}->{X}  * $fac * $max_extra_fac;
		$y_i     = $self->{$eid}->{$issy}->{Y}  * $fac * $max_extra_fac;
		$xy_i    = $self->{$eid}->{$issy}->{XY} * $fac * $max_extra_fac;
		if ($self->{switch_off_D2_error} < 1) {unless ($fac == -1) {print STDERR " *** Error |$nl_incre| factor NL D-2 not -1\n"; print LOG " *** Error |$nl_incre| factor NL D-2 not -1\n";}}
	      }
	    else
	      {
       unless (defined  $self->{$eid}->{$issy}->{X})
       {print LOG " *** Undefined ISSYnum |$coco|GG:$code|ISSY:$issy|DerivedNLCODE:$nl_incre|\n"; $self->{$eid}->{$issy}->{X} = 9876543210;}
		$x_i     = $self->{$eid}->{$issy}->{X}  * $fac;
		$y_i     = $self->{$eid}->{$issy}->{Y}  * $fac;
		$xy_i    = $self->{$eid}->{$issy}->{XY} * $fac;
	      }
	    $ex_1++;
	    if ($ex_1 > 1) 
		   {
		     unless ($self->{warn_if_li_nl_cases_comb} < 1) {print LOG " *** Error Linear and NL Cases Combined $coco \n";}
		   }

	    $x       = $x  + $x_i;
	    $y       = $y  + $y_i;
	    $xy      = $xy + $xy_i;
	    $counter++;

	    my $name = $self->{LCASE}->{$nl_incre}->{segname}->{1};
	    $list_segnames = $list_segnames . ' ' .$name . sprintf("[%6.3fx%-6.1f%8.2f]","$fac","$self->{$eid}->{$issy}->{X}","$x_i");

	    my $seefac = abs($fac);
	    if ($seefac > 1) {print LOG sprintf("%24s %8s %20s %10s%s"," ** WARN CVT NL Factor: ","$fac","$coco","$nl_incre","\n");}
	    #print REPORT "     NLC <$issy> $x_i fac: $fac\n";
	  }
	  
	# 1101 82 00 00 00 00   $ex_2  is the First Incre Position (i.e. 1-5).  $n is the Stepped Factor Code 82 (i.e. 1-9).
	if ($ex_1 > 0)  
	  {
	    $PSTEP = $n; $CSTEP = $i;    # $CSTEP is (coco pos. 1-5)   $n (fac. position 1-8) 
	    my $sv = 0;
	    $sv    = 0.5*($x+$y) + 0.5*($x-$y)*cos(2*$angle) + $xy*sin(2*$angle);
        $sv    = $x  if ($self->{use_stress_column} == 1);
        $sv    = $y  if ($self->{use_stress_column} == 2);
        $sv    = $xy if ($self->{use_stress_column} == 3);  
	  }	  
      } # end of big loop 1-5 incre!

    $self->{stress_value} = 0.5*($x+$y) + 0.5*($x-$y)*cos(2*$angle) + $xy*sin(2*$angle);
    $self->{stress_value} = $x  if ($self->{use_stress_column} == 1);
    $self->{stress_value} = $y  if ($self->{use_stress_column} == 2);
    $self->{stress_value} = $xy if ($self->{use_stress_column} == 3);
	
    $self->{long_list_loadcases} = $list_segnames;

    if (($ex_1 < 1) && ($ex_2 < 5)) {print REPORT " *** ERROR $coco not interpreted!\n"; print LOG " *** ERROR $coco not interpreted!\n";}
  
  } # end sub



  
sub load_loadcase_data_into_structure
  {
    my $self  = shift;
    my $file  = $self->{loadcase_file};
    my $i     = 0;
    my $j     = 0;
    my $type  = 'N';
	my $k     = 1;

    open( INPUT, "< $self->{index_file}" );
    my @contents    = <INPUT>;
    chop(@contents);
    close(INPUT);

    @{$self->{LCASE}->{HEADER}} = ();
    $self->{LCASE}              = {};
    @{$self->{ALLCODES}->{ALLCODES}} = ();
    @{$self->{ISSYNUM}->{NUMBERS}}   = ();
	
    foreach (@contents)
      {
	my $line  = $_;
	if ($_ =~m/^\s*$/) {next;}
	if ($_ =~m/^\s*#/) {push(@{$self->{LCASE}->{HEADER}}, $_); next;}
	$line      =~s/^\s*//;
	$j = 0;
	my @array   = ();
	my $segname;
	my @all     = split('\s+', $line);
	if ($all[0] =~m/EXC/)
	  {
	    $segname = shift(@all);  $segname =~s/\s*//g;
		if ($self->{esg_on} > 0) {my $fem_lc = shift(@all);}
	    @array   = @all; 
	  }
	elsif($all[0] =~m/START/)
	  {
	    $segname = 'STARTOFFLIGHT';
	    for(1 .. 3) {shift(@all);};
		if ($self->{esg_on} > 0) {my $fem_lc = shift(@all);}
	    @array   = @all; 
	  }
	else
	  {
	    $segname = 'segment_' . $i;
	    @array   = @all; 
	  }
	
	foreach (@array)
	  {
	    $array[$j] =~s/\s*//g;
	    $j++;
	  }
	
	for (0 .. 8) {if ((!defined $array[$_]) || ($array[$_] !~m/\d+/)) {$array[$_] = '-0-';}}
	
	my $code       = shift(@array);
	my $issynum    = shift(@array);
	my @factors    = @array;
	my $mission    = unpack('A1 A4', $code); #$issynum = $mission . $issynum;

	if (length($code) > 5) {push(@{$self->{list_of_NL_cases}},$code);}
		
	unless ((defined $code) && (defined $issynum)) {next;}
	$i++;
	if ($code =~m/0$/)   {$self->{STEADY}->{$code}    = $issynum; $self->{STEADY_ISSY}->{$issynum} = $code;}
	if (($code =~m/000$/) && (!defined $self->{DPCASE}->{$mission})) {$self->{DPCASE}->{$mission}  = $issynum;}
	if (defined $self->{LCASE}->{$code}) {$k++;} else {$k = 1;}     # find first time! & find other times!
	
	$self->{LCASE}->{$code}->{mission}->{$k} = $mission;
	$self->{LCASE}->{$code}->{issyno}->{$k}  = $issynum;
	$self->{LCASE}->{$code}->{code}->{$k}    = $code;
	$self->{LCASE}->{$code}->{segname}->{$k} = $segname;
	@{$self->{LCASE}->{$code}->{fac}->{$k}}  = @factors;
	push(@{$self->{ALLCODES}->{ALLCODES}},  $code);
	push(@{$self->{ISSYNUM}->{NUMBERS}}, $issynum);
      }
    $self->{LCASE}->{TOTAL} = $i;
  }

  
  

sub load_stress_input_eid_data # speed without limits
  {
    my $self         = shift;
    my $eid          = $self->{eid};
    my @file         = ();
    my $i            = 1;
    unless (-e $self->{stress_input_file}) {print STDERR " *** Loads File does not Exist!\n"; my $a = <STDIN>; return;}
    print STDERR "  *  loading stresses from:        \= $self->{stress_input_file}\n";
    open( INPUT, "< $self->{stress_input_file}" );
    my @eid_data    = <INPUT>;
    chop(@eid_data);
    close(INPUT);

    foreach (@eid_data)
      {
	my $line = $_;
	if (($_  =~m/^\s*$/) || ($_ =~m/\#/))    { next;}
	#if (($_  =~m/[a-z]/) || ($_ =~m/[A-Z]/)) { next;}
	$line    =~ s/^\s*//g;

	my @data    = split('\s+', $line);
	if ($line =~m/\t+/) {@data    = split('\t+', $line);}
	next unless (defined $data[1]);

	my $issynum    = $data[0];
	my $x          = $data[1];	my $y = 0; 	my $xy = 0;
	$y             = $data[2] if ((defined $data[2]) && ($data[2] !~m/[a-z]/i));
	$xy            = $data[3] if ((defined $data[3]) && ($data[3] !~m/[a-z]/i));
	$issynum       =~s/\s*//g;
	
	if ($self->{use_stress_column} == 1) {$y = 0; $xy = 0;}
	if ($self->{use_stress_column} == 2) {$x = 0; $xy = 0;}
	if ($self->{use_stress_column} == 3) {$x = 0;  $y = 0;}

	if ((defined $issynum) && (defined $x) && ($issynum !~m/[a-z]/i))
	  {
	    $i++;
		my $offset    = 0;
		if (defined $self->{STEADY_ISSY}->{$issynum}) {$offset = $self->{PP_steady_offset}; print LOG "  * 1g Offset applied to ISSY |$issynum|$self->{STEADY_ISSY}->{$issynum}| $x \+ $offset\n";}
	    $self->{$eid}->{$issynum}->{X}   = ($x  + $offset) * $self->{fatigue_fac_all} * $self->{PP_factor};
	    $self->{$eid}->{$issynum}->{Y}   = ($y  + $offset) * $self->{fatigue_fac_all} * $self->{PP_factor};
	    $self->{$eid}->{$issynum}->{XY}  = ($xy + $offset) * $self->{fatigue_fac_all} * $self->{PP_factor};
	    $self->{$eid}->{ISSYNUM}->{$i}   = $issynum;
	  }
      }
	$self->{PP_lower_truncation_value} = $self->{PP_lower_truncation_value} * $self->{fatigue_fac_all} * $self->{PP_factor}; # Now same Unit as STH!
  }


#sub activate_objscan
#  {
#    my $self  = shift;
#    my $mw    = MainWindow->new();
#    $mw->title("Data and Object Scanner");
#    my $scanner   = $mw->ObjScanner(
#				    caller => $self
#				   )->pack(
#					   -expand => 1
#					  );
#  }

  
  
########################### RFORTH #####################################  


sub rfort_sort_and_organize_this_flightblock
  {
     my $self    = shift;
     my $num     = shift;
	 my $name    = 'TF_' . $num; 
     my $j       = 0;
     
     my $valid_flights = $self->{BLOCK}->{VALID};
     my $block         = 1.0;
	 my @class_data    = @{$self->{BLOCK}->{DATA}};
     my $stress_points = $self->{BLOCK}->{POINTS};;
     #my $i             = $self->{NOW}->{POINTS};

     print RFORTOUT sprintf("%10.2f%10.2f  %s", "$valid_flights","$block","\n");

      my @values      = @{$self->{BLOCK_STRESSES}->{DATA}};
      @{$self->{new_values}} = ();

      foreach (@values)
         {
             #print STDERR "|$_|\n";
             if ($_ ne "")
               {
                  push(@{$self->{values}}, $_); 
               }
         }

      my $ommit    = $self->{RFORTH_value};
      $self->{tro} = @{$self->{values}}[0];
      $self->{pea} = @{$self->{values}}[0];

      $self->{f} = 0;
      $self->{j} = 0;
      $self->{n} = $stress_points;

      for (1 .. $self->{n})                                  #   10
        {
          my $num = $_;
          my $value   = shift(@{$self->{values}});
          #$value = sprintf("%12.6f","$value")  if (defined $value);
          #print STDERR "$value\n";
          
          if (!defined $value)
            {         
                      #print STDERR " ** |$value| undefined value! continue ? \n"; my $a = <STDIN>;
                      $self->{j} = 256;
                      last;
            }

          if ($value > $self->{pea})           #       30
            {
                      $self->{pea}  = $value;
                      my $a = $self->{pea} - $self->{tro};

                      if ($a <= $ommit)
                        {
                          next;
                        }
                      else
                        {
                          push(@{$self->{new_values}}, $self->{tro});
                        }

                      &gimme_MAXZO($self, $value);       # call sub # 200
            }
          elsif ($value < $self->{tro})          # 40
            {
                      $self->{tro}  = $value;
                      my $a = $self->{pea} - $self->{tro};
                      
                      if ($a < $ommit)
                        {
                          next;
                        }
                      else
                        {                 
                          push(@{$self->{new_values}}, $self->{pea});
                        }

                      &gimme_MINZO($self, $value);       # call sub   #100
            }
        }

      if ($self->{f} == 1)
        {             
          push(@{$self->{new_values}}, $self->{pea});
        }
      elsif ($self->{f} == -1)
        {             
          push(@{$self->{new_values}}, $self->{tro});
        }
      else
        {
          my $c = ($self->{pea} + $self->{tro}) / 2.0;                                
          push(@{$self->{new_values}}, $c);
        }

      &write_the_output_rfort($self, $name);
  }




sub  write_the_output_rfort
  {
    my $self   = shift;
    my $name   = shift;
    my @data   = @{$self->{new_values}};
    my $points = $#data + 1;
    my $i      = 0;

    print RFORTOUT sprintf("%10i %58s %10s%s", "$points","","$name","\n");

    foreach (@data) 
      {
	my $value = $_;
	$i++;
	print RFORTOUT sprintf("%10.2f", "$value");

	if ($i == 8) 
	  {
	    $i  = 0;
	    print RFORTOUT "\n";	
	  }
      }

    unless ($i == 0) 
      {
	$i  = 0;
	print RFORTOUT "\n";
      }
  }
   


sub gimme_MAXZO
  {
    my $self     = shift;
    my $value    = shift;
    my $ommit    = $self->{RFORTH_value};

    $self->{f} = 1;

     for (1 ... $self->{n})        # 210
      {
	$value = shift(@{$self->{values}});
	#$value = sprintf("%12.6f","$value") if (defined $value);

	if (!defined $value)
	  {
	    $self->{j} = 256; #print STDERR " ** |$value| Maxzo undefined value! continue ? \n"; my $a = <STDIN>;
	    last;
	  }

	if ($value > $self->{pea})
	  {
	    $self->{pea}  = $value;
	    next;
	  }
	
	my $b = $self->{pea} - $value;
	
	if ($b > $ommit)
	  {	  
	    push(@{$self->{new_values}}, $self->{pea});
	    $self->{tro} = $value;
	    last;
	  }
      }

    &gimme_MINZO($self, $value) unless ($self->{j} > 0);       # call sub   #100
  }




sub gimme_MINZO
  {
    my $self  = shift;
    my $value = shift;
    my $ommit    = $self->{RFORTH_value};

    $self->{f} = -1;

    for (1 ... $self->{n})      # 110
      {
	$value = shift(@{$self->{values}});
	#$value = sprintf("%12.6f","$value")  if (defined $value);
	
	if (!defined $value)
	  {
	    $self->{j} = 256;  #print STDERR " ** |$value| Minzo undefined value! continue ? \n"; my $a = <STDIN>;
	    last;
	  }

	if ($value < $self->{tro})
	  {
	    $self->{tro}  = $value;
	    next;
	  }

	my $b = $value - $self->{tro};
	
	if ($b > $ommit)
	  {
	    push(@{$self->{new_values}}, $self->{tro});
	    $self->{pea} = $value;
	    last;
	  }
      }

    &gimme_MAXZO($self, $value) unless ($self->{j} > 0);       # call sub # 200
  }

 
########################################################################################################################################
  
sub write_the_updated_rforth_CLASS_version  
  { 
     my $self        = shift;
     my $num         = shift; 
     my $points      = $self->{BLOCK}->{POINTS};
     my $valid       = $self->{BLOCK}->{VALID};
     my $block       = $self->{BLOCK}->{BLOCK};
     my $header      = $self->{BLOCK}->{HEAD};	 
     my @class_data  = @{$self->{BLOCK}->{DATA}}; 
     my @all_peaks   = @{$self->{BLOCK_STRESSES}->{DATA}};
     my @rforthpeaks = @{$self->{new_values}};
     my $p   = $#rforthpeaks + 1;  $self->{CLASS}->{$num}->{NEWPOINTS} = $p;

     my $rv  = shift(@rforthpeaks); 
     print CLASSNEW sprintf("%-200s%s","$header","\n");
     print CLASSNEW sprintf("%8i%s","$p","\n");
     print CLASSNEW sprintf("%12i%8i%s","$valid","$block","\n");
     print CLASSLOG sprintf("TF: %5i %5i %10i  %-200s%s","$num","$valid","$points","$header","\n");
          
     foreach(@class_data)
           {
             my $classline = $_;
             my $value   = shift(@all_peaks);                                     
             #$value      = sprintf("%12.6f","$value") if (defined $value);                   
             if ((defined $rv) && ($rv == $value)) 
                {
                   print CLASSNEW "     $classline\n";                                                        
                   print CLASSLOG sprintf("%10.5f    %s  %35s%s","$value","|","$classline","\n"); 
                   $rv    = shift(@rforthpeaks);                                       
                }
             else
                {
                   print CLASSLOG sprintf("%10.5f  RM%s  %35s%s","$value","|","$classline","\n");
                }
          }
     $self->{ALL_TF_HEADERS}->{$num} = $self->{BLOCK}->{HEAD};   
     $self->{ALL_TF_VALID}->{$num}   = $self->{BLOCK}->{VALID};
     $self->{ALL_TF_BLOCK}->{$num}   = $self->{BLOCK}->{BLOCK};	 
  }

  
  
sub write_COMPLETED_FLUGABALAUF_file_USER_requested_ALL
  { 
     my $self          = shift;
     my $miniANA_file  = $self->{flugablauf_file};  
	 $miniANA_file     =~s/\..+$//;
     $miniANA_file     = $miniANA_file . '_' . $self->{job_name} . '_RfExD.ana';
     print STDERR  "  *... Please wait ... Process requires Time ....\n";
     my $block         = 1;
     my @logmini_files = @{$self->{mini_files}};
	 if ($#logmini_files < 1) {$miniANA_file = $self->{eid} . '_' . $self->{job_name} . '_RfExD.ana';}  # ANA output name will be same as STF if only 1 STF was used for the Job! 
     my $key           = $#logmini_files + 1;
	 @{$self->{newdata}} = (); @{$self->{deleted}} = (); 
     open(ANAFILE,  ">" .$miniANA_file); 
	 open(DELETED,  "> ALL_DELETED_PEAKS.log"); 
     my @rforthvalue  =  @{$self->{USER}->{RFORTH}};
     my @factorvalue  = @{$self->{USER}->{FAC_PP}};
     my @steadyoffsetvalue     = @{$self->{USER}->{GG_OFFSET}};
     my @lowertruncationvalue  = @{$self->{USER}->{LOWER_TRUNCATION}};  
	 
     print ANAFILE "# ------------------------------------------------------------------\n";
     print ANAFILE "# Reduced *.ANA File based on RFORTH-EXTENDED Method [ANA Baseline: $self->{flugablauf_file}]: Job Date = $self->{date}\n#";     
     foreach(@logmini_files) 
	     { 
			my $rf = shift(@rforthvalue); my $f = shift(@factorvalue); my $gos = shift(@steadyoffsetvalue); my $ltv = shift(@lowertruncationvalue);
			print ANAFILE " $_ [$rf|$f|$gos|$ltv] ";
		 }
     print ANAFILE "\n# Refer to Job \= $self->{job_name} for Details [Affected Flights: $self->{attack_only_these_flights}] [Special Events Secured: $self->{keep_these_events_always}] \n";  
     print ANAFILE "# ------------------------------------------------------------------\n";
     
     if ($self->{total_flights} > $self->{run_only_till_flight_num}) {$self->{total_flights} = $self->{run_only_till_flight_num};}
     my @fh;
	 my $n = -1;
	 for my $i (0..$#logmini_files) {open $fh[$i], "<", $logmini_files[$i] or die $!;}	# open all files

	 while (1) # read one line from each file
	    {
          my @lines;
          push @lines, scalar readline $fh[$_] for 0..$#logmini_files;
          last unless defined $lines[0];	 #print STDERR "@lines"; #my $std = <STDIN>;
          chomp @lines;
		  my $type   = 'Body';
		  my $line   = 'xxxxxx'; 
		  my $ignore =  0;
		  foreach (@lines) 
		    {
	            $line  = $_; 
	            if ($line =~m/TF/) {$type = 'Header';}
                if ($line =~m/RM/) {$ignore++;}
			}
		  if ($type =~m/Header/)
		    {   
			    $n++;
				&write_ANA_REDUCED_file_contents_for_this_flight($self, $n) unless ($n == 0);
			}
		  if ($type eq 'Body') 
            {	my $fn = $n + 1;
				if (defined $self->{attack_only_these_flights_numbers}->{$fn}) 
				  {
			        if ($ignore < $key) {push(@{$self->{newdata}}, $line);} else {push(@{$self->{deleted}}, $line);}
				  }
				else {push(@{$self->{newdata}}, $line);}  
            }				   
        }
	 $n = $n + 1; # Get the last Flight!	
     &write_ANA_REDUCED_file_contents_for_this_flight($self, $n);
     close(ANAFILE);
	 close(DELETED);
  } # end sub


  
sub write_COMPLETED_FLUGABALAUF_file_USER_requested_SINGLE
  { 
     my $self          = shift; 
     print STDERR  "  *... Please wait ... Process requires Time ....\n";
     my $block         = 1;
	 
     my @logmini_files = $self->{single_mini_file};
	 my $miniANA_file  = $self->{eid} . '_' . $self->{job_name} . '_RfExD.ana';  # ANA output name will be same as STF if only 1 STF was used for the Job! 
     my $key           = $#logmini_files + 1;
	 @{$self->{newdata}} = (); @{$self->{deleted}} = (); 
     open(ANAFILE,  ">" .$miniANA_file); 
	 open(DELETED,  ">" .$miniANA_file ."_ALL_DELETED_PEAKS.log"); 
     my @rforthvalue  =  @{$self->{USER}->{RFORTH}};
     print ANAFILE "# ------------------------------------------------------------------\n";
     print ANAFILE "# Reduced *.ANA File based on RFORTH-EXTENDED Method [ANA Baseline: $self->{flugablauf_file}]: Job Date = $self->{date}\n#";     
     foreach(@logmini_files) 
	     { 
			my $rf = shift(@rforthvalue);  print ANAFILE " $_ [$rf] ";
		 }
     print ANAFILE "\n# Refer to Job \= $self->{job_name} for Details [Affected Flights: $self->{attack_only_these_flights}] [Special Events Secured: $self->{keep_these_events_always}] \n";  
     print ANAFILE "# ------------------------------------------------------------------\n";
     
     if ($self->{total_flights} > $self->{run_only_till_flight_num}) {$self->{total_flights} = $self->{run_only_till_flight_num};}
     my @fh;
	 my $n = -1;
	 for my $i (0..$#logmini_files) {open $fh[$i], "<", $logmini_files[$i] or die $!;}	# open all files

	 while (1) # read one line from each file
	    {
          my @lines;
          push @lines, scalar readline $fh[$_] for 0..$#logmini_files;
          last unless defined $lines[0];	 #print STDERR "@lines"; #my $std = <STDIN>;
          chomp @lines;
		  my $type   = 'Body';
		  my $line   = 'xxxxxx'; 
		  my $ignore =  0;
		  foreach (@lines) 
		    {
	            $line  = $_; 
	            if ($line =~m/TF/) {$type = 'Header';}
                if ($line =~m/RM/) {$ignore++;}
			}
		  if ($type =~m/Header/)
		    {   
			    $n++;
				&write_ANA_REDUCED_file_contents_for_this_flight($self, $n) unless ($n == 0);
			}
		  if ($type eq 'Body') 
            {	my $fn = $n + 1;
				if (defined $self->{attack_only_these_flights_numbers}->{$fn}) 
				  {
			        if ($ignore < $key) {push(@{$self->{newdata}}, $line);} else {push(@{$self->{deleted}}, $line);}
				  }
				else {push(@{$self->{newdata}}, $line);}  
            }				   
        }
	 $n = $n + 1; # Get the last Flight!
     &write_ANA_REDUCED_file_contents_for_this_flight($self, $n);
     close(ANAFILE);
	 close(DELETED);
  } # end sub

    
  
  
sub write_ANA_REDUCED_file_contents_for_this_flight
  {
     my $self          = shift;  
     my $num           = shift;                        
	 my $valid         = $self->{ALL_TF_VALID}->{$num};
	 my $block         = $self->{ALL_TF_BLOCK}->{$num};  print STDERR "  * Processing Flight: $num \n";
	 
     my $peaks  = $#{$self->{newdata}} + 1;
     my $header = $self->{ALL_TF_HEADERS}->{$num};
     print ANAFILE sprintf("%-200s%s","$header","\n");
     print ANAFILE sprintf("%9i%s","$peaks","\n");
     print ANAFILE sprintf(" %12i %6i%s","$valid","$block","\n");	
     		 
     foreach(@{$self->{newdata}}) 
         {
            my @b = split('\|', $_);   #my @b = unpack('A15 A55', $_);
            print ANAFILE "  $b[1]\n";			 
         }

     my $deaks  = $#{$self->{deleted}} + 1;
     print DELETED sprintf("%-200s%s","$header","\n");
     print DELETED sprintf("%9i%s","$deaks","\n");
     print DELETED sprintf(" %12i %6i%s","$valid","$block","\n");
		 
     foreach(@{$self->{deleted}}) 			 
		 { 
		    my @d = split('\|', $_);
		    print DELETED "  $d[1]\n";
		 }
	 @{$self->{newdata}} = (); @{$self->{deleted}} = (); 		 
  }



  
########################################################################################### 
