#! /opt/perl5/bin/perl -w

use strict;
use POSIX qw(sin cos ceil floor log10 atan);
#use Tk;
#use Tk::ObjScanner;

print STDERR "  * Assess & ADAPTS DRFs in CDFs. This Tool is Based on Architecture from status |Count_Flugablauf v7.8| !\n";
print STDERR "  * Note: Only to be deployed to users after dedicated relevant Spectra Training for understanding DRFs!\n";
print STDERR "  * DRF Range (recommended): Variable for \$self->{modify_drf_ratio} between 0.5 to 2.0! i.e. Half to Double original DRF value!\n";
print STDERR "  * Rules & Guarantees & Limitations: \n";
print STDERR "  *    ---->>>> Flight Type Concept unaffected: Assessments undertaken independently per Flight, per Event Cycle, and per Step (i.e. 1 to 8)\n";
print STDERR "  *    ---->>>> Spectra Shape Unaffected: No changes to DRFs of non-affected Events , even when combined with affected Event \n";
print STDERR "  *    ---->>>> Max & Min Unaffected: No changes to GAG Cycle in all Flights & per Flight! \n";
print STDERR "  *    ---->>>> Conservative: If conflict with above Rules: Less cycles removed OR More cycles added than requested  by user!\n";
print STDERR "  *    ---->>>> Note when reducing   DRF: Peaks are not Removed in ANA file - Just the Event is Removed!\n";
print STDERR "  *    ---->>>> Note when increasing DRF: New Peaks are Added to ANA file   - No new combinations are created!\n";
print STDERR "  *    ---->>>> Rule for adding Cycles (after v1.08):  Inserts a complete cycle of an Event (then 1g added) before the original Event!\n";
print STDERR "  *    ---->>>> RESTRICTIONs: [a]. DRF Ratio between 0.5 (half) to 2.0 (double)! [b]. Only use for Events with 2 directions e.g. VG, LG etc\n";


my $self                  = {};
$self->{event_directions} = 2;   # ONLY 2 DIRECTIONS !!!!!

$self->{tool_version}     = '1.08';
$self->{tool_date}        = '22.08.2017'; 
$self->{user_inputfile}   = '00_adapt_DRF_inputfile.txt';  # Please provide all Inputs in this inputfile!
$self->{error}            = 0;

if (defined $ARGV[0])  {$self->{user_inputfile} = $$ARGV[0]; print STDERR "using inputfile: $ARGV[0]\n";} # Now also accepts \$ARGV[0] from Command Line
&read_inputfile_from_user($self);

##############################  YSou would not normally need to Edit below this line ##################################
# MANUAL INPUT: Uncomment the next 6 lines to run this Script directly!
# $self->{spectra_txt_file}         = 'A350XWB1000TL-111OWBPY-MR2.txt';  # #'loadcases_C26.txt'; 'A350XWB900-526NF-MR3.txt'
# $self->{spectra_ana_file}         = 'A350XWB1000TL-111OWBPY-MR2.ana';  # #'flugablauf_C26.ana'; 'A350XWB900-526NF-MR3.ana';
# $self->{fatigue_event_affected}   = 'EXC_VG';
# $self->{modify_drf_ratio}        =  0.54;        # i.e. 1.6/3.0 = 0.54
# $self->{run_only_till_flight_num} =  1e6;
# $self->{add_comments_to_anafile}  =  0;    # Set this to 0! This should only be set to 1 whendebugging! 

# THIS is the expected format for USER INPUTFILE!
# begin GLOBAL_INPUTS

             # date                       =   10-05-2017
             # spectra_txt_file           =   A350XWB1000TL-111OWBPY-MR2.txt
             # spectra_ana_file           =   A350XWB1000TL-111OWBPY-MR2.ana
             # fatigue_event_affected     =   EXC_VG
             # DRF_OLD_value              =     3.0  
             # DRF_NEW_value              =     1.6
             # run_only_till_flight_num   =     1e6
             # add_comments_to_anafile    =       0    

# end GLOBAL_INPUTS   
#########################################################################################################################
print STDERR "   |$self->{spectra_ana_file}|$self->{spectra_txt_file}|\n";
unless ((-e $self->{spectra_ana_file}) && (-e $self->{spectra_txt_file})) {print STDERR " *** Not all Input Files Exist!"; $self->{error}++;}
return if ($self->{error} > 0);

$self->{all_events_occur_data}    = {};
$self->{cvt_factor_warn}          = 10;      # [warn if CVT factor higher than this value!]
$self->{print_special_fsftest}    = 0;       # 1 or 0

$self->{modify_drf_ratio}         = $self->{DRF_NEW_value} / $self->{DRF_OLD_value};
$self->{modify_drf_ratio}         = sprintf("%3.2f","$self->{modify_drf_ratio}");

my $dn        = $self->{modify_drf_ratio};  $dn =~s/\.//;
my $an        = $self->{spectra_ana_file};  $an =~s/\.ana.*$//;
my $corename  = '_' . $self->{fatigue_event_affected} . '_DRF_' . sprintf("%03i","$dn");

$self->{drf_output}         = $an . $corename . '.ana';
$self->{err_file}           = $an . $corename . '.stderr';
$self->{log_file}           = $an . '_original.statistics';
$self->{drf_file}           = $an . '_original.drf';

$self->{dp_max}                   =  0;
$self->{complete_validity}        =  0;
$self->{total_points_all_flight}  =  0;
$self->{txt_cvt_esg_switch}       = 'TXT';
@{$self->{global_considerations}} = ();
$self->{switch_off_D2_error}      =  0;
$self->{mission_ratio_mixed}      = {};

open(ERR,     ">" . $self->{err_file});
open(LOG,     ">" . $self->{log_file});
open(DRF,     ">" . $self->{drf_file});
open(OUTPUT,  ">" . $self->{drf_output});

&load_loadcase_data_into_structure_count_only($self);
&load_one_FT_Block_from_flugabluaf_and_process_count_only($self, 'DRF');
&load_one_FT_Block_from_flugabluaf_and_process_count_only($self, 'MAKE');  

close(ERR);
close(LOG);
close(DRF);
close(OUTPUT);
print STDERR "  * end of process!";

#&activate_objscan($self);
#MainLoop;

############################ DRF ################################

sub write_summary_statistics_only_BIG_flights_plus_DRF
  {
    my $self      = shift;
	my $num       = shift;
    my $prev      = 00000;
	my $appf      = $self->{BLOCK}->{POINTS} / $self->{BLOCK}->{VALID};  $appf = sprintf("%10.2f","$appf");
    print LOG "______________________________________________\n";
    print LOG "  * All Flights should have been processed!\n";
    print LOG "  * Deltap max.value: $self->{dp_max}         \n";
    print LOG "  * Peaks: $self->{BLOCK}->{POINTS}   Spectra Validity: $self->{complete_validity}\n";
    print LOG "  * Average Points/Flight: $appf [only for info for this flight!]\n";
    print LOG "_________________ statistics _________________\n";
	
    foreach (@{$self->{ALLCODES}->{ALLCODES}})
      {
	my $code   = $_;
	my $length = length($code);
	my $i      = 0;		
	if ($prev == $code)   {next;}
	if ($code  =~m/000$/) {my $issynum = $self->{LCASE}->{$code}->{issyno}->{1}; print LOG sprintf("%-24s%-9s%-5s%6s%11s%s","DPCASE","$code","$issynum","dp","N/A","\n"); next;}
	$self->{total_occur_per_code}->{$code} = 0;
                
    my $gg_alone = 0;
    if (defined $self->{gg_codes_alone}->{$code}) 
       {
         $gg_alone = $self->{gg_codes_alone}->{$code};
       }
				
	if (defined $self->{statistics}->{GG}->{$code})
	  {
	    my $occur   = $self->{statistics}->{GG}->{$code};
	    my $segname = $self->{LCASE}->{$code}->{segname}->{1}; 
		my $issynum = $self->{LCASE}->{$code}->{issyno}->{1};
		if ($gg_alone > 0)
		  {
		    my $mn       = $self->{LCASE}->{$code}->{mission}->{1};
		    my $gg_ratio = $gg_alone ;
	        print LOG sprintf("%-24s%-9s%-5s%6s%11i [%5i UnComb] [%3.2f/UnCFT]%s","$segname","$code","$issynum","steady","$occur","$gg_alone","$gg_ratio","\n") ;
            print DRF sprintf("%4s_steady_ %-29s%-9s%s","","$segname","$code","\n");
			$self->{all_events_occur_data}->{$num}->{steady_name}->{$code} = $segname;
		  }
	    print LOG sprintf("%-24s%-9s%-5s%6s%11i [%5i UnComb] WARN! WHY?%s","$segname","$code","$issynum","steady","$occur","$gg_alone","\n") if ($gg_alone == 0);		
        print LOG sprintf("%-24s%-9s%-5s%6s%11i%s","$segname","$code","$issynum","steadyA","$gg_alone","\n") unless ($self->{print_special_fsftest} < 1);
	    next;
	  }
###########	  
	for (1 .. 9) 
	  {
	    my $d = $_;
	
	    if (defined $self->{statistics}->{$d}->{$code})
	      {
		$i++;
		my $occur   = $self->{statistics}->{$d}->{$code};
		my $segname = $self->{LCASE}->{$code}->{segname}->{$d};
		my $issynum = $self->{LCASE}->{$code}->{issyno}->{$d};
		if ($length > 5) {$segname = $self->{LCASE}->{$code}->{segname}->{1}; $issynum = $self->{LCASE}->{$code}->{issyno}->{1};}  # NL Cases!	
		if (defined $segname)
		  {
		    print LOG sprintf("%-24s%-9s%-5s%6s%11i","$segname","$code","$issynum","neg","$occur") if ($d =~m/2/);               # ($d =~m/2|4|6|8/);
		    print LOG sprintf("%-24s%-9s%-5s%6s%11i","$segname","$code","$issynum","pos","$occur") if ($d =~m/1|3|4|5|6|7|8|9/); # ($d =~m/1|3|5|7|9/);
			print DRF sprintf("%10s_incre_ %-24s%-9s%s",    "","$segname","$code","\n");
			print DRF sprintf("%10s_incre_ %-24s%-9s%11i%s","","$segname","$code","$occur","\n") if ($length > 5);
			$self->{all_events_occur_data}->{$num}->{incre_name}->{$code}->{$d} = $segname;
		  }
		else
		  {
		    print LOG sprintf("%-24s%-9s|D:%2s|%11i","UndeFinTXT_eXisTinANA","$code","$d","$occur");
		  }
		  
		$self->{total_occur_per_code}->{$code} = $self->{total_occur_per_code}->{$code} + $occur;
		
		for (0 .. 8)
		  {
		    if (defined $self->{stats_more}->{$d}->{$_}->{$code})
		      {
			my $occur_i = $self->{stats_more}->{$d}->{$_}->{$code};
			print LOG sprintf("%8i","$occur_i");
			my $step = $_ + 1; # Remember that arraya starts with 0 index.
			print DRF sprintf("  %16s|%1i %1i| %-8i %s","","$d","$step","$occur_i","\n");  # pos|neg direction vs. factor position with Occurence!
			$self->{all_events_occur_data}->{$num}->{incre_occur}->{$code}->{$d}->{$_} = $occur_i;
		      } else {print LOG sprintf("%8s","");}
		  }
		print LOG "\n";	
	      }
	  }

	unless ($i > 0)
          {
	    my $segname = $self->{LCASE}->{$code}->{segname}->{1};
		my $issynum = $self->{LCASE}->{$code}->{issyno}->{1};
	    if (defined $segname) 
	      {print LOG sprintf("%-24s%-9s%-5s%s","$segname","$code","$issynum","\n");}
	    else 
	      {print LOG sprintf("%-24s%-9s%-5s%6s%11s%s","*** ERROR ***","$code","$issynum","N/A","N/A","\n");}
          }	
        $prev = $code;
      }
    print LOG "_________________ statistics big _________________   \n";
  }


sub load_one_FT_Block_from_flugabluaf_and_process_count_only
  {
    my $self              = shift;
    my $wass              = shift;
    my $file              = $self->{spectra_ana_file};
	@{$self->{order_of_flights_in_ana}} = ();
    print STDERR "  * loading flugablauf:  \= $self->{spectra_ana_file} | \n  * assessing ...\n";

    open(FLUGAB,  "<" . $file);
    my $type   = 0;
    my $no     = -1;
    my $jump   = 0;
	
    while (<FLUGAB>)
      {
	chop($_);
	if (($_ =~m/^\s*$/) || ($_ =~m/^\s*\#/)) {print LOG "$_\n"; if ($wass =~m/MAKE/i) {print OUTPUT sprintf("%-80s |Event_DRF: %-10s Ratio: %3.2f|v%3.2f|%s", "$_","$self->{fatigue_event_affected}","$self->{modify_drf_ratio}","$self->{tool_version}","\n");}; next;}

	my $line = $_;
	$line    =~s/^\s*//;

	if ($line =~m/^TF/)
	  {
	    $no++;
	    $type   = 1;

	    if ($no > 0)
	      {
		  if ($no > $self->{run_only_till_flight_num}) {$jump = 1; last;};
		&process_the_current_flugablauf_block_DRF_only($self,$no) if ($wass eq 'DRF');
		&process_the_current_flugablauf_block_MAKE_only($self,$no) if ($wass eq 'MAKE');
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
    unless($jump > 0) 
	  {
	    &process_the_current_flugablauf_block_DRF_only($self,$no)  if ($wass eq 'DRF');
	    &process_the_current_flugablauf_block_MAKE_only($self,$no) if ($wass eq 'MAKE');
	  }
	$self->{total_flight_types} = $no;
    close(FLUGAB);
    print STDERR "\n  * activity loop completed! \n";
  }



sub process_the_current_flugablauf_block_DRF_only
  {
    my $self     = shift;
    my $num      = shift;
    my $i        = 0;

    my $points = $self->{BLOCK}->{POINTS};
    my $valid  = $self->{BLOCK}->{VALID};
    my $block  = $self->{BLOCK}->{BLOCK};
    my @data   = @{$self->{BLOCK}->{DATA}};
    my @longcodes           = ();
    my $real_points = $#data + 1;
    @{$self->{DP_TF_ARRAY}} = ();
	@{$self->{TH_TF_ARRAY}} = ();
	$self->{mission_now}    = 100;
	my $catch_rotation      = 0;
	my $catch_mlg_impact    = 0;
	
    # CLEARS MEMORY for NEXT FLIGHT INFO!!!
	$self->{statistics} = {}; $self->{stats_more}        = {};
	$self->{dp_max}     =  0; $self->{complete_validity} = 0;


    $self->{BLOCK}->{HEAD} =~m/TF_(.+)\s*\(/;
    my $header = $1;
	push(@{$self->{order_of_flights_in_ana}}, $header); 

    $self->{complete_validity}       = $self->{complete_validity} + $valid * $block;
    $self->{total_points_all_flight} = $self->{total_points_all_flight} + $points * $valid * $block;
    print ERR sprintf("TF:%5i   valid:%5i   block:%3i   Points:%8i %-100s%s", 
		      "$num",  "$valid",  "$block",   "$points", "$self->{BLOCK}->{HEAD}","\n");
	
    if ($real_points != $points) {print ERR " *** ERROR: TFnum: $num [$header] Points_in_File: $points ! Use_Real_Points: $real_points \n";}
    #print STDERR sprintf("%4s%12s %s","","$header","\n");  
 
    @{$self->{GG_ORDER_THIS_FLIGHT}}            = ();	
	$self->{check_segment_sequence_this_flight} = {};
	unless (defined $self->{mlg_is_in_this_flight}->{$num}) {$self->{mlg_is_in_this_flight}->{$num} = 0;}
	unless (defined $self->{nlg_is_in_this_flight}->{$num}) {$self->{nlg_is_in_this_flight}->{$num} = 0;}

    foreach (@data)
      {
	my $line = $_;
	$line    =~s/^\s*//;
	if ($line =~m/[a-z]/) {print ERR " *** ERROR code ignored $line\n"; next;}
	my ($code,$coco,$dp,$temp) = split('\s+', $line);
	&analyze_coco_fourteen_position_count_only($self, $coco, $dp, $temp, $valid, $num);
	push(@{$self->{DP_TF_ARRAY}}, $dp); push(@{$self->{TH_TF_ARRAY}}, $temp); push (@longcodes, $coco);

	# check if flight starts/ends with a 1g
	if ($i == 0)      {unless ($coco =~m/0000000000$/) {push(@{$self->{global_considerations}}, " *** ERROR Flight does not START with 1g: $self->{BLOCK}->{HEAD}");};}
	if ($i == $#data) {unless ($coco =~m/0000000000$/) {push(@{$self->{global_considerations}}, " *** ERROR Flight does not END   with 1g: $self->{BLOCK}->{HEAD}");};}
	$i++;
	# check consistency of codes per line!
	my $lenA = length($code);  my $lenB = length($coco); #print STDERR "$lenA $lenB\n";
	unless (($lenA == 4) && ($lenB == 14))  {push(@{$self->{global_considerations}}, " *** Inconsistent Order of Codes |$line| $self->{BLOCK}->{HEAD}");}
	unless  ($coco =~m/^$code/)             {push(@{$self->{global_considerations}}, " *** Inconsistent CodeCoCo Found |$line| $self->{BLOCK}->{HEAD}");}
	unless (($dp =~m/\./)&&($temp =~m/\./)) {push(@{$self->{global_considerations}}, " *** DP/THER value not RealValue |$line| $self->{BLOCK}->{HEAD}");}                
	# check consistency of mission number for each line for the complete flight
	my $m_now = unpack('A1A3', $code);
	if ($self->{mission_now} == 100) {$self->{mission_now} = $m_now;}
	unless($m_now == $self->{mission_now}) {push(@{$self->{global_considerations}}, " *** ERROR Multi-MISSIONS in 1 FLIGHT |$line| $self->{BLOCK}->{HEAD}");}
	$self->{this_mission_has_thes_number_of_ftypes}->{$m_now}->{$num} = $valid;   # The number of flight types with this mission is stored here!

	 my $lcode = 'NONE';  
     if (defined @{$self->{GG_ORDER_THIS_FLIGHT}}[0]) {my @a = @{$self->{GG_ORDER_THIS_FLIGHT}}; $lcode = $a[$#a];}	   
     unless (defined $self->{check_segment_sequence_this_flight}->{$code}) {push(@{$self->{GG_ORDER_THIS_FLIGHT}}, $code);}	
	 if    ((defined $self->{check_segment_sequence_this_flight}->{$code}) && ($lcode ne $code)) {push(@{$self->{GG_ORDER_THIS_FLIGHT}}, $code); print LOG "  *** ERROR Flight $num |$line| This SegCode happens 2-wice in same Flight!\n";} # This will Trigger Warnings!
     $self->{check_segment_sequence_this_flight}->{$code} = 1;	   
      } # end loop @data

    # WRITES STATISTICS PER FLIGHT HERE!
	print LOG sprintf("TF:%5i   valid:%5i   block:%3i   Points:%8i %-100s%s",  "$num",  "$valid",  "$block",   "$points", "$self->{BLOCK}->{HEAD}","\n");
	print DRF sprintf("%sBEGIN_HEADER: %-40s %sVALID: %5i %sPEAKS: %8i%s","\n","$self->{BLOCK}->{HEAD}","\n", "$valid","\n","$points","\n");
	&write_summary_statistics_only_BIG_flights_plus_DRF($self, $num);
	$self->{statistics_combi}  = {};   $self->{statistics_combi_count} = {}; 
	@{$self->{DP_JUMP_WARN}}   = ();   @{$self->{DP_LOW_WARN}}         = (); 
	
	unless (defined $self->{mission_ratio_mixed}->{$self->{mission_now}}) {$self->{mission_ratio_mixed}->{$self->{mission_now}} = 0;}
	$self->{mission_ratio_mixed}->{$self->{mission_now}} = $self->{mission_ratio_mixed}->{$self->{mission_now}} + $valid * $block;
	
  }


  
sub analyze_coco_fourteen_position_count_only
  {
    my $self   = shift;
    my $coco   = shift;
    my $dp     = shift;
    my $temp   = shift;
    my $valid  = shift;
	my $fnum   = shift;
    my @a      = ();

    $coco      =~m/(\d\d\d\d)(\d\d)(\d\d)(\d\d)(\d\d)(\d\d)/;

    $a[0]      =  $1;
    $a[1]      =  $2;
    $a[2]      =  $3;
    $a[3]      =  $4;
    $a[4]      =  $5;
    $a[5]      =  $6;
	
    if(length($coco) < 14) {print ERR "\n *** ERROR |$coco| not standard length!\n";}
	
    my $code     = shift(@a);
    my $gg_code  = $code . '0';

    if (defined $self->{max_dp_per_steady_1g}->{$gg_code})
      {
	my $v_max = $self->{max_dp_per_steady_1g}->{$gg_code};
	my $v_min = $self->{min_dp_per_steady_1g}->{$gg_code};
	if ($dp > $v_max)
	  {
	    $self->{max_dp_per_steady_1g}->{$gg_code} = $dp;
	  }

	if ($dp < $v_min)
	  {
	    $self->{min_dp_per_steady_1g}->{$gg_code} = $dp;
	  }
      }
    else
      {
	$self->{max_dp_per_steady_1g}->{$gg_code} = $dp;
	$self->{min_dp_per_steady_1g}->{$gg_code} = $dp;
      }

    unless (defined  $self->{statistics}->{GG}->{$gg_code}) {$self->{statistics}->{GG}->{$gg_code} = 0;}
    $self->{statistics}->{GG}->{$gg_code} = $self->{statistics}->{GG}->{$gg_code} + $valid;
	$self->{flights_validity_seen}->{$gg_code}->{$fnum} = $valid;

    my $i        = 0;
    my $counter  = 0;
    my $ex_1     = 0;
    my $ex_2     = 0;
    my $ex_3     = 0;

    if ($self->{dp_max} < $dp) {$self->{dp_max} = $dp;}

    if ($coco =~m/0000000000/)
      {
         if (defined $self->{gg_codes_alone}->{$gg_code}) {$self->{gg_codes_alone}->{$gg_code} = $self->{gg_codes_alone}->{$gg_code} + $valid;}
         else {$self->{gg_codes_alone}->{$gg_code} = $valid;}
      }

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
		
	    unless (defined $self->{statistics}->{$d}->{$incre}) {$self->{statistics}->{$d}->{$incre} = 0;};
	    unless (defined $self->{stats_more}->{$d}->{$n-1}->{$incre}) {$self->{stats_more}->{$d}->{$n-1}->{$incre} = 0;};
	    $self->{statistics}->{$d}->{$incre} = $self->{statistics}->{$d}->{$incre} + $valid;
	    $self->{stats_more}->{$d}->{$n-1}->{$incre} = $self->{stats_more}->{$d}->{$n-1}->{$incre} + $valid;
	    $ex_1++;
	    $counter++;
		
	    my $seefac = abs($fac);
	    if ($seefac > $self->{cvt_factor_warn}) {print ERR " ** WARN CVT Factor: $fac  $coco  $incre \n";}
		$self->{flights_validity_seen}->{$d}->{$incre}->{$fnum} = $valid;
	  }

	if (($d == 2) && (!defined $self->{LCASE}->{$incre}->{issyno}->{2}))
	  {
	    if (defined $self->{LCASE}->{$incre}->{issyno}->{1}) 
	      {
		my $issy = $self->{LCASE}->{$incre}->{issyno}->{1};
		my $fac  = @{$self->{LCASE}->{$incre}->{fac}->{1}}[$n-1] * -1.0;

		unless (defined $self->{statistics}->{$d}->{$incre}) {$self->{statistics}->{$d}->{$incre} = 0;};
		unless (defined $self->{stats_more}->{$d}->{$n-1}->{$incre}) {$self->{stats_more}->{$d}->{$n-1}->{$incre} = 0;};
		$self->{statistics}->{$d}->{$incre}  = $self->{statistics}->{$d}->{$incre} + $valid;
		$self->{stats_more}->{$d}->{$n-1}->{$incre} = $self->{stats_more}->{$d}->{$n-1}->{$incre} + $valid;
		$ex_1++;
		$counter++;

		my $seefac = abs($fac);
		if ($seefac > $self->{cvt_factor_warn}) {print ERR " ** WARN CVT Factor: $fac  $coco  $incre \n";}
		$self->{flights_validity_seen}->{$d}->{$incre}->{$fnum} = $valid;
	      }
	  }

	my $nl_incre = $code . $i . $ip;
	if ($ex_1 > 0) {$ex_3 = 2}

	if (defined $self->{LCASE}->{$nl_incre}->{issyno}->{1})
	  {
	    my $issy = $self->{LCASE}->{$nl_incre}->{issyno}->{1};
	    my $fac  = @{$self->{LCASE}->{$nl_incre}->{fac}->{1}}[0];

	    if ($ip =~m/2$/) # Maxdam adds a -1 again to NLC of the 2nd direction!!!!!!!
	      {
		unless ($fac == -1)
		    {
			   if ($self->{switch_off_D2_error} < 1) 
			     {
				   push(@{$self->{global_considerations}}, " *** GLOBAL WARNING Check your NL D-2 Factors e.g. |$nl_incre| factor is not -1 ?"); 
			       $self->{switch_off_D2_error} = 1; 
				 }
			}
	      }

	    $ex_1++;
	    unless (defined $self->{statistics}->{$d}->{$nl_incre}) {$self->{statistics}->{$d}->{$nl_incre} = 0};
	    $self->{statistics}->{$d}->{$nl_incre} = $self->{statistics}->{$d}->{$nl_incre} + $valid;
	    if ($ex_3 > 2) {print ERR " ** WARN Linear and NL Cases are Combined: $coco \n";}
	    $counter++;

	    my $seefac = abs($fac);
	    unless ($seefac == 1) {print ERR " ** ERROR CVT NonLinear Factor should be +/- 1.0: $fac  $coco  $nl_incre \n";}
		$self->{flights_validity_seen}->{$d}->{$nl_incre}->{$fnum} = $valid;
				
        my $segname = $self->{LCASE}->{$nl_incre}->{segname}->{1};	#print LOG " FOUND MLG $self->{mlg_is_in_this_flight}->{$fnum}\n";
        if (($segname =~m/\_LMLG/) || ($segname =~m/\_MLG/) || ($segname =~m/\_BWLG/)) {$self->{mlg_is_in_this_flight}->{$fnum} = $self->{mlg_is_in_this_flight}->{$fnum} + 1;}
        if  ($segname =~m/\_LNLG/) {$self->{nlg_is_in_this_flight}->{$fnum} = $self->{nlg_is_in_this_flight}->{$fnum} + 1;}
      }
      }

    if (($ex_1 < 1) && ($ex_2 < 5))
      {
	print ERR " *** ERROR $coco not interpreted!\n";
	print LOG " *** ERROR $coco not interpreted!\n";	
      }

    if ($counter > 1)
      {
	my $i = 0;  my $a  = 'A';  #print LOG "  * more than 1 linear incre are combined in this coco: $coco\n";
	foreach (@a)
	  {
	    $i++;
	    my $ip        = $_; if ($ip =~m/00/) {next;}
	    my ($n, $d)   = unpack('A1A1', $ip);
	    my $li_incre  = $code . $i; 
		my $nl_incre  = $code . $i . $ip;
		my $incre     = 'A';
		if ((defined $self->{LCASE}->{$nl_incre}->{issyno}->{1}) && ($self->{LCASE}->{$nl_incre}->{issyno}->{1} > 0))  {$incre = $nl_incre;}		
		if ((defined $self->{LCASE}->{$li_incre}->{issyno}->{1}) && ($self->{LCASE}->{$li_incre}->{issyno}->{1} > 0))  {$incre = $li_incre;}
		
	    if ((defined $self->{LCASE}->{$incre}->{issyno}->{1}) && ($self->{LCASE}->{$incre}->{issyno}->{1} > 0))
	      {
		   $self->{statistics_combi}->{$incre}->{$a} = 1;
		   unless ($a eq 'A')
		     {
		       unless (defined $self->{statistics_combi_count}->{$incre}->{$a}) {$self->{statistics_combi_count}->{$incre}->{$a} = 0;}
		       $self->{statistics_combi_count}->{$incre}->{$a} = $self->{statistics_combi_count}->{$incre}->{$a} + $valid;
		     }
		   $a = $incre;
	      } 
	  } # end @a
      } # end counter > 1
  } # end subroutine



sub load_loadcase_data_into_structure_count_only
  {
    my $self  = shift;
    my $file  = $self->{loadcase_file};
    my $i     = 0;
    my $j     = 0;
    my $type  = 'N';
	my $k     = 1;
    my $pcode = '0000'; my $pissy = '0000'; my $psign = 1; my $pseg = 'SEG';
    open( INPUT, "< $self->{spectra_txt_file}" );
    my @contents    = <INPUT>;
    chop(@contents);
    close(INPUT);
	
    $self->{esg_switch}               = 0;
	
    @{$self->{LCASE}->{HEADER}} = ();
    $self->{LCASE}              = {};
    @{$self->{ALLCODES}->{ALLCODES}} = ();
    @{$self->{ISSYNUM}->{NUMBERS}}   = ();
    @{$self->{ALLCODES}->{ALLNAMES}} = ();	
	for (1 ..9) {@{$self->{ALLCODES}->{GG_ORDER}->{$_}} = ();}
	
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
	if ($all[0] =~m/EXC|N|\_/)
	  {
	    $segname = shift(@all);  $segname =~s/\s*//g;
	    @array   = @all; 
	  }
	elsif($all[0] =~m/START\_/)      
	  {
	    $segname = 'STARTOFFLIGHT';
	    for(1 .. 1) {shift(@all);};
	    @array   = @all; 
	  }	
    elsif($all[0] =~m/START/i)      
	  {
	    $segname = 'STARTOFFLIGHT';
	    for(1 .. 3) {shift(@all);};
	    @array   = @all; 
	  } 
	else
	  {
	    $segname = 'segment_' . $i; $self->{txt_cvt_esg_switch}    = 'CVT';
	    @array   = @all;
	  }
	
	# EXC_N=1_TXOT_F1      1GTXOT___4      11010 508  # Just for the A340/A330 ESG Project  !
	if ($self->{esg_switch} > 0) {shift(@array); $self->{txt_cvt_esg_switch} = 'ESG';};
	
	foreach (@array)
	  {
	    $array[$j] =~s/\s*//g;
	    $j++;
	  }
	
	for (0 .. 8) {if ((!defined $array[$_]) || ($array[$_] !~m/\d+/)) {$array[$_] = '-0-';}}
	
	my $code       = shift(@array);
	my $issynum    = shift(@array);
	my @factors    = @array;
	my ($mission,$gf,$snum,$pos)    = unpack('A1 A1 A2 A1', $code); #$issynum = $mission . $issynum;
	if ($segname =~m/segment_/) {$segname = $mission . '_segment_' . $snum;}
		
	unless ((defined $code) && (defined $issynum)) {next;}
	$i++;
	if ($code =~m/0$/)   {$self->{STEADY}->{$code}    = $issynum;  unless($code =~m/000$/) {push(@{$self->{ALLCODES}->{GG_ORDER}->{$mission}}, $code);}}
	if ($code =~m/000$/) {$self->{DPCASE}->{$mission} = $issynum;}
	if (defined $self->{LCASE}->{$code}) {$k = $self->{LCASE_K}->{$code} + 1;} else {$k = 1;}     # find first time! & find other times!
    if ((defined $self->{LCASE}->{$code}) && ($pcode != $code)) {print LOG sprintf("%68s |%-110s| %-45s%s","  ** ERROR Can ISAMI inteprete Position/Order of this CODE in TXT/CVT","$line","Warning may be caused by another Reason!","\n");}
	if ($pos > 5) {print LOG sprintf("%68s |%-110s| %-45s%s","  ** ERROR This INCRE Position greater than 5!","$line","CLASS has max 5 INCRE positions!","\n");}
	$self->{LCASE}->{$code}->{mission}->{$k} = $mission;
	$self->{LCASE}->{$code}->{issyno}->{$k}  = $issynum;
	$self->{LCASE}->{$code}->{code}->{$k}    = $code;
	$self->{LCASE}->{$code}->{segname}->{$k} = $segname;
    $self->{LCASE_K}->{$code}                = $k;           # HTP GLA order may not be as others!
	@{$self->{LCASE}->{$code}->{fac}->{$k}}  = @factors;
	push(@{$self->{ALLCODES}->{ALLCODES}},  $code);
	push(@{$self->{ALLCODES}->{ALLNAMES}},  $segname);	
	push(@{$self->{ISSYNUM}->{NUMBERS}}, $issynum);
	######
	unless (defined $self->{know_all_unique_issy_num}->{$issynum}) {$self->{know_all_unique_issy_num}->{$issynum} = 1;} else {$self->{know_all_unique_issy_num}->{$issynum} = $self->{know_all_unique_issy_num}->{$issynum} + 1;}
	push(@{$self->{issy_num_unique_array}->{$issynum}}, $code);	
	push(@{$self->{class_code_unique_array}->{$code}}, $segname);
	push(@{$self->{issy_num_unique_segnames}->{$issynum}}, $segname);	
	######
	my $length = length($code);	
	######
	if ($length == 5)   # check how many Events are attached to this 1g 
	  {
	    my ($core, $pos) = unpack('A4 A1', $code);  # 22012
		unless (defined $self->{LINEAR_CODES}->{$core})  {$self->{LINEAR_CODES}->{$core} = 1;} else {$self->{LINEAR_CODES}->{$core} = $self->{LINEAR_CODES}->{$core} + 1;}
		push(@{$self->{linear_codes_segnames}->{$core}}, sprintf("%-18s","$segname"));
		if ($pos > 5) {print LOG sprintf("%68s |%-110s| %s","  *** ERROR INCRE in Position |$pos| - CLASS has MAX 5 INCRE!!!!!!","$line", "\n");}
	  }	
	######
    if ($length > 5)
	  {
	    my ($core, $pos, $x, $d) = unpack('A4 A1 A1 A1', $code);  # 2201211
		my ($c,    $incre)       = unpack('A5 A2',       $code);  # 2201212
		$self->{NL_CODES}->{$code}    = @{$self->{LCASE}->{$code}->{fac}->{1}}[0]; # this is the factor!;
		$self->{NL_UNIQUE}->{$core}->{$pos}->{$d}->{$incre}    = $code;
	  }
	######
	my $flg = 0;
	my $prv  = 10000000;	my $sign = 1;
	foreach(@factors)
	   {
          next if ($_ =~m/-0-/);  if ($_ <  0) {$sign = -1;}
		  my $a = abs($_);
		  unless ($a < $prv) {$flg++;}
		  $prv  = $a;
	   }                      
    unless ($flg == 0) {push(@{$self->{LCASE_TXT_CVT_WARN}}, sprintf("  ** ERROR %-21s    ---> Sequence for Factors Un-Expected     %-55s","$segname","|@factors|"));}
	$pcode  = $code; $pissy = $issynum;	$psign = $sign; $pseg = $segname;
      }
    $self->{LCASE}->{TOTAL} = $i;
  }
  

 
# sub activate_objscan
  # {
    # my $self  = shift;
    # my $mw    = MainWindow->new();
    # $mw->title("Data and Object Scanner");
    # my $scanner   = $mw->ObjScanner(
				    # caller => $self
				   # )->pack(
					   # -expand => 1
					  # );
  # }

############################ MAKE ################################


sub process_the_current_flugablauf_block_MAKE_only
  {
    my $self     = shift;
    my $num      = shift;
    my $i        = 0;

    my $points = $self->{BLOCK}->{POINTS};
    my $valid  = $self->{BLOCK}->{VALID};
    my $block  = $self->{BLOCK}->{BLOCK};
    my @data   = ();

    print STDERR "   * running... TF_$num \n";
	print ERR    sprintf("%-10s%s", "$self->{BLOCK}->{HEAD}","\n");
	
	my $counter    = 0;
	
    foreach (@{$self->{BLOCK}->{DATA}})
      {
		my $line = $_;
		$line    =~s/^\s*//;		
		my ($code,$coco,$dp,$temp) = split('\s+', $line);
        my @a      = ();
		$coco      =~m/(\d\d\d\d)(\d\d)(\d\d)(\d\d)(\d\d)(\d\d)/;
 	    $a[0]      =  $1;
   		$a[1]      =  $2;
   		$a[2]      =  $3;
 	  	$a[3]      =  $4;
   		$a[4]      =  $5;
   		$a[5]      =  $6;
		
	    my $mission  = unpack('A1 A4', $code);
        my $gg_code  = $code . '0';
        my $issy_gg  = $self->{STEADY}->{$gg_code};	
        my $ignore   = 0;
		
        for (1 .. 5)  
         {
		    my $i          = $_;
	        my $ip         = $a[$_];   #@a
	        my ($step, $d) = unpack('A1A1', $ip);
	        my $inc_code   = $code . $i;
	        if ($ip =~m/00/) {next;}
			my $inc_name   = 'NA';
			my $add_drf    = 0;
			if   (defined $self->{all_events_occur_data}->{$num}->{incre_name}->{$inc_code}->{$d}) {$inc_name = $self->{all_events_occur_data}->{$num}->{incre_name}->{$inc_code}->{$d}} 
			elsif(defined $self->{all_events_occur_data}->{$num}->{incre_name}->{$inc_code}->{1})  {$inc_name = $self->{all_events_occur_data}->{$num}->{incre_name}->{$inc_code}->{1}}
			
			#if (defined $self->{LCASE}->{$inc_code}->{segname}->{$d}) {$inc_name = $self->{LCASE}->{$inc_code}->{segname}->{$d}} else {$inc_name = $self->{LCASE}->{$inc_code}->{segname}->{1}}			
			#print OUTPUT "$inc_code $inc_name\n";
			
            if ($inc_name =~m/$self->{fatigue_event_affected}/) 
			  {
			    #print OUTPUT "Match Name: $inc_name | $self->{fatigue_event_affected} | $inc_code | DIR: $d | STEP: $step | $coco\n";
			    if (defined $self->{all_events_occur_data}->{$num}->{incre_occur}->{$inc_code}->{$d}->{$step-1})	
				  {
				    #print OUTPUT "Defined Name: $inc_name | $inc_code | DIR: $d | STEP: $step |\n";
				    my $occur_i = $self->{all_events_occur_data}->{$num}->{incre_occur}->{$inc_code}->{$d}->{$step-1};  # factorSteps 1-8 are in array 0-7 !!!!!!
                    unless (defined $self->{all_events_occur_data}->{$num}->{incre_occur_now}->{$inc_code}->{$d}->{$step-1}) 
					  {
					    $self->{all_events_occur_data}->{$num}->{incre_occur_now}->{$inc_code}->{$d}->{$step-1} = $occur_i;
					  } 
					elsif ($self->{modify_drf_ratio} >= 1) {$self->{all_events_occur_data}->{$num}->{incre_occur_now}->{$inc_code}->{$d}->{$step-1} = $self->{all_events_occur_data}->{$num}->{incre_occur_now}->{$inc_code}->{$d}->{$step-1} + $valid;}	
					elsif ($self->{modify_drf_ratio} <  1) {$self->{all_events_occur_data}->{$num}->{incre_occur_now}->{$inc_code}->{$d}->{$step-1} = $self->{all_events_occur_data}->{$num}->{incre_occur_now}->{$inc_code}->{$d}->{$step-1} - $valid;}						

                    my $lim_occur  = $occur_i * $self->{modify_drf_ratio} * 1.01;
                    if ($self->{modify_drf_ratio} >  1) {unless ($self->{all_events_occur_data}->{$num}->{incre_occur_now}->{$inc_code}->{$d}->{$step-1} > $lim_occur) {$add_drf =  1; $counter++;}}	
                    if ($self->{modify_drf_ratio} <  1) {unless ($self->{all_events_occur_data}->{$num}->{incre_occur_now}->{$inc_code}->{$d}->{$step-1} < $lim_occur) {$add_drf = -1;}}
					if ($self->{modify_drf_ratio} == 1.0) {$add_drf = 0;}
				  }	
				if (($counter == $self->{event_directions}) && ($self->{modify_drf_ratio} >  1)) {$add_drf = 0; $counter = 0;}
			  }
			
			if ($add_drf > 0)  # ADD
              {
				my ($f, $d) = unpack('A1A1', $a[$i]);
				for (1 .. $self->{event_directions})  # Note: $self->{event_directions} is 2 for Symmetrical Events!
				  {
				    my $ncoco = $code;
				    my $ndir  = $_;
					for (1 .. 5)
					  {
					    if ($_ == $i) {$ncoco = $ncoco . $f. $ndir;} else {$ncoco = $ncoco . '00';} 
						#print OUTPUT "$_ | $i | $ncoco ADD\n";
					  }
					my $nline = "NA";
					if ($self->{add_comments_to_anafile} > 0) {$nline = sprintf("%-6s%14s %6.2f %6.2f %8s %28s","$code","$ncoco","$dp","$temp", "ADDED_LINE", "$inc_name");} #!!!!!!!!! DEBUGGING ONLY !!!!!!!!!!!
				    else                                      {$nline = sprintf("%-6s%14s %6.2f %6.2f","$code","$ncoco","$dp","$temp");}   #2150  21500000220000 611.00   0.00 
					push(@data, $nline);
					print ERR sprintf("%-6s%14s %6.2f %6.2f %8s %28s %s","$code","$ncoco","$dp","$temp", "ADDED_LINE", "$inc_name","\n");		
				  }
			  }

			if ($add_drf < 0) # REMOVE
              {
				unless ($self->{all_events_occur_data}->{$num}->{incre_occur}->{$inc_code}->{$d}->{$step-1} == $valid)
				  {
				    my $ncoco = $code;
				    $ignore   = 1;
				    for (1 .. 5)
				     {
				       if ($_ == $i) {$ncoco = $ncoco . '00';} else {$ncoco = $ncoco . $a[$_];}
				       #print OUTPUT "$_ | $i | $ncoco REM\n";
				     }
					my $nline = "NA"; 
				    if ($self->{add_comments_to_anafile} > 0) {$nline = sprintf("%-6s%14s %6.2f %6.2f %8s %28s","$code","$ncoco","$dp","$temp", "REMOVED_EVENT", "$inc_name");}   #!!!!!!!!! DEBUGGING ONLY !!!!!!!!!!!
			        else                                      {$nline = sprintf("%-6s%14s %6.2f %6.2f","$code","$ncoco","$dp","$temp");}   #2150  21500000220000 611.00   0.00
				    push(@data, $nline);
				    print ERR sprintf("%-6s%14s %6.2f %6.2f %8s %28s %s","$code","$ncoco","$dp","$temp", "REMOVED_EVENT_HERE", "$inc_name","\n"); 
				  }
			  }			  
         }
		push(@data, $line) unless ($ignore > 0);	
	  }
    
	# Write the updated Data!
    my $new_points = $#data +1;	
    print OUTPUT sprintf("%-10s%s",     "$self->{BLOCK}->{HEAD}","\n");
    print OUTPUT sprintf(" %8i%s",      "$new_points","\n");
    print OUTPUT sprintf(" %12i %6i%s", "$valid","$block","\n");
	
    foreach (@data)
      {
        print OUTPUT "     $_\n";  # space in front to remain as in orig. data!
      }	

  }

 
 
 
sub read_inputfile_from_user
  {
    my $self  = shift;

    open(INPUT,  "<". $self->{user_inputfile}) or print STDERR "  *** User Inputfile not Found |$self->{user_inputfile}|\n";
    my @input = <INPUT>;
    chop(@input);
    close(INPUT);
    my $type = 'NA';
    foreach (@input)
     {
	   if (($_ =~m/^\s*$/) || ($_ =~m/^\s*#/)) {next;}

	   if ($_ =~m/begin\s*GLOBAL_INPUTS/i) {$type = 'GLOBAL'; next;}
	   if ($_ =~m/^\s*end/i)               {$type = 'NA';     next;}
	   if ($type eq 'GLOBAL') 
         {
	        my ($key, $value) = split('\s*=\s*', $_);
	        if((defined $key) && (defined $value))
	          {
	            $key           =~s/\s*//g;
                if ($value =~m/#/) {$value =~s/^\s*//; my @a = split('#', $value); $value = $a[0]; $value =~s/\s*//g;} else {$value =~s/^\s*//; $value =~s/\s*$//;} 
                $self->{$key}  = $value; #print STDERR "|$key|$value|\n";
	          } 
         }
     }

	my @all_keys = ('spectra_txt_file','spectra_ana_file','fatigue_event_affected','DRF_OLD_value','DRF_NEW_value','run_only_till_flight_num','add_comments_to_anafile',);

	foreach(@all_keys)
	 {
	   my $key = $_;
	   unless (defined $self->{$key}) {print STDERR " *** Undefined/Unrecognized Variable for: |$key|\n"; $self->{error}++;}
	 }
  }
  
  
 
