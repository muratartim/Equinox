#!/usr/local/bin/perl -w

use strict;
use POSIX qw(log10 ceil floor);

my $self                    = {};
my $root                    = $self->{ROOT};

$self->{input}              = $ARGV[0];
$self->{ommit_value}        = $ARGV[1];

&start($self);

sub start
{
  my $self   = shift;
  unless (-e  "$self->{input}")
  {
    print STDERR " *** STH file not found!\n";
    return;
  }
  
  &rfort_ommit_sth_file_data($self);
}

sub load_all_sth_data
  {
    my $self   = shift;
    my $stress_factor = 1.0;
    my $column = 8.0;
    my $sum    = 0.0;
    my $sth    = $self->{now_use_this_sth_file};

    $self->{maximum_stress} = -1000000;
    $self->{minimum_stress} =  1000000;

    @{$self->{header}} = ();
    @{$self->{names}} = ();

    open( INPUT, "< $sth" );
    my @contents    = <INPUT>;
    chop(@contents);
    close(INPUT);

    foreach (0 .. 3) 
      {
	my $line = shift(@contents);
	push(@{$self->{header}}, $line);
      }

    for(1 .. 100001)                     # Max FT number expected.
      {
	my $i = $_;
	
	my $line = shift(@contents);
	last if (!defined $line);

	$line =~ s/^\s*//g;

	my ($valid_flight, $blocks) = split(/\s+/, $line);

	$line = shift(@contents);
	$line =~ s/^\s*//g;
	
	my ($steps, $name)          = split(/\s+/, $line);

	$steps              =~s/\s*//;
       #$name               = 'TF_' . $i  unless (defined $name);
	$name               = 'TFLIGHT_' . $i;
	$name               =~s/\s*//;

	push(@{$self->{names}}, $name);
	$self->{$name}->{steps} = $steps;
	$self->{$name}->{valid} = $valid_flight;
	$self->{$name}->{block} = $blocks;
	
	my $rows  = $steps / $column;
	my $int   = floor($rows); # gives the Interger part of Number
	my $dig   = ceil($rows);  # makes 30.25 to become 31
	$rows     = $dig;
	
	for (1 .. $rows) 
	  {
	    $line = shift(@contents);
	    push(@{$self->{$name}->{data}}, $line);
	  }

	my $j    = 0;
	foreach (@{$self->{$name}->{data}})
	  {
	    my @data = split(/\s+/,$_);

	    foreach (@data)
	      {
		if ($_ ne "")
		  {	
		    my $value = $stress_factor * $_;
		    $j++;
		    $self->{$name}->{values}->{$j} = $value;
		    if ($value    > $self->{maximum_stress})
		      {
			$self->{maximum_stress} = $value;
			$self->{maximum_ftype}  = $name;
			$self->{maximum_plotn}  = $i;
		      }
		    if ($value    < $self->{minimum_stress})
		      {
			$self->{minimum_stress} = $value;
			$self->{minimum_ftype}  = $name;
			$self->{minimum_plotn}  = $i;
		      }
		  }
	      }
	  }

	$sum = $sum + $valid_flight * $blocks;
      }

    $self->{VALID_FOR} = $sum;
    print STDERR "BLOCKs:  \n @{$self->{names}} \n";
  }

# end of program.


#########################  RFORT SUBROUTINES  ##########################




sub rfort_ommit_sth_file_data
  {
    my $self = shift;
    my $file =  $self->{now_use_this_sth_file} = $self->{input};

    $self->{rfort_output} =  $self->{input} . '.rfort';

    print STDERR " * begin rfort for: $file \n";

    &load_all_sth_data($self);
    &rfort_sort_and_organize_flighttypes($self);

    print STDERR " * ... end rfort\n";
  }








sub rfort_sort_and_organize_flighttypes
  {
    my $self          = shift;
    $self->{nmax}     = 0;

    open( RFORTOUT, "> $self->{rfort_output}" );

    print RFORTOUT "@{$self->{header}}[0] \n";
    print RFORTOUT "max: $self->{maximum_stress}   min: $self->{minimum_stress}\n";
    print RFORTOUT "validfor \= $self->{VALID_FOR}   |$self->{input}|\n";
    print RFORTOUT "RFORT from < E_PURE > Process. Value Ommitted: $self->{ommit_value}\n";

    foreach (@{$self->{names}})
      {
	my $name          = $_;
	my @values;
	my $valid_flights = $self->{$name}->{valid};
	my $block         = $self->{$name}->{block};
	my $stress_points = $self->{$name}->{steps};
	my $i             = 0;

	print RFORTOUT sprintf("%10.2f%10.2f  %s", "$valid_flights","$block","\n");

	for (1 .. $stress_points)
	  {
	    $i  = $_;

	    push(@values, $self->{$name}->{values}->{$i});
	  }

	$self->{$name}         = {};
 	@{$self->{values}}     = @values;
	@{$self->{new_values}} = ();

	my $ommit    = $self->{ommit_value};
	$self->{tro} = $values[0];
	$self->{pea} = $values[0];

	$self->{f} = 0;
	$self->{j} = 0;
	$self->{n} = $i;

	for (1 .. $self->{n})                                  #   10
	  {
	    my $value = shift(@{$self->{values}});

	    if (!defined $value)
	      {
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

    close(RFORTOUT);
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
    my $ommit    = $self->{ommit_value};

    $self->{f} = 1;

     for (1 ... $self->{n})        # 210
      {
	$value = shift(@{$self->{values}});

	if (!defined $value)
	  {
	    $self->{j} = 256;
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
    my $ommit    = $self->{ommit_value};

    $self->{f} = -1;

    for (1 ... $self->{n})      # 110
      {
	$value = shift(@{$self->{values}});

	if (!defined $value)
	  {
	    $self->{j} = 256;
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


############################ end RFORT #########################









1;

